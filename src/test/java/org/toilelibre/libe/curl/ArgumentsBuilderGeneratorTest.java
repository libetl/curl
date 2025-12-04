package org.toilelibre.libe.curl;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.apache.commons.cli.Option;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.toilelibre.libe.curl.Curl.CurlArgumentsBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class ArgumentsBuilderGeneratorTest {

    private static final Pattern      WORD_SEPARATOR = Pattern.compile ("-([a-zA-Z])");
    private static final Pattern      DIGITS_PATTERN = Pattern.compile ("-([0-9]+)");
    private static final List<String> DIGITS         = Arrays.asList ("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine");

    @Test
    public void addOptionsToArgumentsBuilder () throws NotFoundException, CannotCompileException, IOException {
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
            argsBuilderClass.addMethod (this.builderOptionMethod (argsBuilderClass, stringType, longMethodName, option.getLongOpt (), option.hasArg ()));
            if (!shortMethodName.equals (longMethodName)) {
                argsBuilderClass.addMethod (this.builderOptionMethod (argsBuilderClass, stringType, shortMethodName, option.getOpt (), option.hasArg ()));
            }
        }

        argsBuilderClass.writeFile ("target/classes");
    }

    @Test
    public void addOptionsToReadmeMarkdown () throws IOException {
        final int shortNamesMaxLength = Arguments.ALL_OPTIONS.getOptions ().stream ().map (Option::getOpt)
                .map (String::length).max (Integer::compareTo).orElse (0);
        final int longNamesMaxLength = Arguments.ALL_OPTIONS.getOptions ().stream ().map (Option::getLongOpt)
                .map (String::length).max (Integer::compareTo).orElse (0);
        final int descriptionMaxLength = Arguments.ALL_OPTIONS.getOptions ().stream ().map (Option::getDescription)
                .map (String::length).max (Integer::compareTo).orElse (0);

        StringBuilder tableBuilder = new StringBuilder ();
        tableBuilder.append ("| Short Name ").append (IntStream.range (1, Math.max (0, shortNamesMaxLength - 9))
                .mapToObj (i -> " ").collect (joining ()));
        tableBuilder.append ("| Long Name ").append (IntStream.range (1, Math.max (0, longNamesMaxLength - 8))
                .mapToObj (i -> " ").collect (joining ()));
        tableBuilder.append ("| Argument Required ");
        tableBuilder.append ("| Description ").append (IntStream.range (0, Math.max (0, descriptionMaxLength - 11))
                .mapToObj (i -> " ").collect (joining ())).append ("|\n");
        tableBuilder.append ("| ").append (IntStream.range (1, Math.max (0, shortNamesMaxLength) + 1)
                .mapToObj (i -> "-").collect (joining ())).append (' ');
        tableBuilder.append ("| ").append (IntStream.range (1, Math.max (0, longNamesMaxLength) + 1)
                .mapToObj (i -> "-").collect (joining ())).append (' ');
        tableBuilder.append ("| ----------------- ");
        tableBuilder.append ("| ").append (IntStream.range (1, Math.max (0, descriptionMaxLength) + 1)
                .mapToObj (i -> "-").collect (joining ())).append (" |\n");

        for (final Option option : Arguments.ALL_OPTIONS.getOptions ()) {
            tableBuilder.append ("| ").append (option.getOpt ())
                    .append (IntStream.range (1, Math.max (0, shortNamesMaxLength - option.getOpt ().length () + 2))
                    .mapToObj (i -> " ").collect (joining ()));
            tableBuilder.append ("| ").append (option.getLongOpt ())
                    .append (IntStream.range (1, Math.max (0, longNamesMaxLength - option.getLongOpt ().length () + 2))
                            .mapToObj (i -> " ").collect (joining ()));
            tableBuilder.append ("| ").append (option.hasArg ())
                    .append (IntStream.range (1, Math.max (0, 19 - (option.hasArg () ? 4 : 5)))
                            .mapToObj (i -> " ").collect (joining ()));
            tableBuilder.append ("| ").append (option.getDescription ().replace ('|', ','))
                    .append (IntStream.range (1, Math.max (0, descriptionMaxLength - option.getDescription ().length () + 2))
                            .mapToObj (i -> " ").collect (joining ())).append ("|\n");
        }
        final File readme = stream (Objects.requireNonNull (new File (ArgumentsBuilderGeneratorTest.class
                .getProtectionDomain ().getCodeSource ().getLocation ().getFile ()).getParentFile ().getParentFile ()
                .listFiles ())).filter (f -> "README.md".equalsIgnoreCase (f.getName ()))
                .findFirst ().orElseThrow (() -> new FileNotFoundException ("README.md"));
        final String readmeContent = IOUtils.toString (new FileInputStream (readme));

        final String newReadmeContent = Pattern.compile (
                "Supported arguments \\(so far\\) :")
                .splitAsStream (readmeContent).findFirst ()
                .orElse ("") + "Supported arguments (so far) :\n\n" + tableBuilder.toString ();

        FileWriter writer = new FileWriter (readme);
        writer.write (newReadmeContent);
        writer.close ();
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
            final StringBuilder replacement = new StringBuilder ();
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
