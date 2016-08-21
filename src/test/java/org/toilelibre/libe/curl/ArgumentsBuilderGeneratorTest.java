package org.toilelibre.libe.curl;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.Option;
import org.junit.Test;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class ArgumentsBuilderGeneratorTest {

    private static final Pattern WORD_SEPARATOR = Pattern.compile ("-([a-zA-Z])");

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
        final String lowerCased = "" + Character.toLowerCase (opt.charAt (0)) + opt.substring (1);
        final StringBuffer result = new StringBuffer ();
        final Matcher matcher = ArgumentsBuilderGeneratorTest.WORD_SEPARATOR.matcher (lowerCased);

        while (matcher.find ()) {
            matcher.appendReplacement (result, matcher.group (1).toUpperCase ());
        }
        matcher.appendTail (result);
        return result.toString ();
    }
}
