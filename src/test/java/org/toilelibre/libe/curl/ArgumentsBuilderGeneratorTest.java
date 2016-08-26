package org.toilelibre.libe.curl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.Option;
import org.junit.Test;
import org.toilelibre.libe.curl.Curl.CurlArgumentsBuilder;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class ArgumentsBuilderGeneratorTest {

    private static final Pattern      WORD_SEPARATOR = Pattern.compile ("-([a-zA-Z])");
    private static final Pattern      DIGITS_PATTERN = Pattern.compile ("-([0-9]+)");
    private static final List<String> DIGITS         = Arrays.asList ("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine");

    @Test
    public void addOptionsToArgumentsBuilder () throws ClassNotFoundException, NotFoundException, CannotCompileException, IOException {

        final ClassPool pool = ClassPool.getDefault ();
        final CtClass argsBuilderClass = pool.get (CurlArgumentsBuilder.class.getName ());
        final CtClass stringType = pool.get (String.class.getName ());
        argsBuilderClass.defrost ();

        final String methodName = this.methodNameOf (Arguments.ALL_OPTIONS.getOptions ().iterator ().next ().getLongOpt ());
        if (argsBuilderClass.getDeclaredMethods (methodName).length > 0) {
            return;
        }

        for (final Option option : Arguments.ALL_OPTIONS.getOptions ()) {
            final String shortMethodName = this.methodNameOf (option.getOpt ());
            final String longMethodName = this.methodNameOf (option.getLongOpt ());
            argsBuilderClass.addMethod (this.builderOptionMethod (argsBuilderClass, stringType, shortMethodName, option.getOpt (), option.hasArg ()));
            if (!shortMethodName.equals (longMethodName)) {
                argsBuilderClass.addMethod (this.builderOptionMethod (argsBuilderClass, stringType, longMethodName, option.getLongOpt (), option.hasArg ()));
            }
        }

        argsBuilderClass.writeFile ("target/classes");
    }

    private CtMethod builderOptionMethod (final CtClass ctClass, final CtClass stringType, final String methodName, final String optName, final boolean hasArg) throws CannotCompileException {
        final CtMethod method = new CtMethod (ctClass, methodName, hasArg ? new CtClass [] { stringType } : new CtClass [0], ctClass);
        method.setBody ("{curlCommand.append (\"-" + (optName.length () == 1 ? "" : "-") + optName + " \"" + (hasArg ? " + $1 + \" \"" : "") + ");\n" + "    " + "return $0;}");
        return method;
    }

    private String methodNameOf (final String opt) {
        if ((opt.length () == 1) && (opt.charAt (0) >= 'A') && (opt.charAt (0) <= 'Z')) {
            return opt.toLowerCase () + "UpperCase";
        }

        final String lowerCased = ("" + Character.toLowerCase (opt.charAt (0)) + opt.substring (1)).replace ('.', '-');
        final String notStartingWithADigitAndLowerCased = this.removeLeadingDigits ('-' + lowerCased);
        return this.capitalizeParts (notStartingWithADigitAndLowerCased.replaceAll ("-", ""));
    }

    private String removeLeadingDigits (final String lowerCased) {
        final StringBuffer result = new StringBuffer ();
        final Matcher matcher = ArgumentsBuilderGeneratorTest.DIGITS_PATTERN.matcher (lowerCased);

        while (matcher.find ()) {
            final StringBuffer replacement = new StringBuffer ();
            for (int i = 0; i < matcher.group (1).length (); i++) {
                replacement.append (ArgumentsBuilderGeneratorTest.DIGITS.get (matcher.group (1).charAt (i) - '0'));
            }
            matcher.appendReplacement (result, replacement.toString ());
        }
        matcher.appendTail (result);
        return result.toString ();
    }

    private String capitalizeParts (final String notStartingWithADigitAndLowerCased) {
        final StringBuffer result = new StringBuffer ();
        final Matcher matcher = ArgumentsBuilderGeneratorTest.WORD_SEPARATOR.matcher (notStartingWithADigitAndLowerCased);

        while (matcher.find ()) {
            matcher.appendReplacement (result, matcher.group (1).toUpperCase ());
        }
        matcher.appendTail (result);
        return result.toString ();
    }
}
