package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.reflect.*;
import org.junit.*;

import java.lang.reflect.*;
import java.util.*;

import static java.util.Arrays.*;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

public class CachedArgumentsTest {

    private static void setInternalState (
            final Object receiver,
            final String fieldName,
            final Object value) {
        final Field f = FieldUtils.getField (receiver instanceof Class<?> ? (Class<?>) receiver :
                receiver.getClass (), fieldName, true);
        FieldUtils.removeFinalModifier (f);
        try {
            f.set (receiver, value);
        } catch (IllegalAccessException e) {
            throw new org.junit.AssumptionViolatedException ("Could not inject some value", e);
        }
    }

    private static int containsKeyCallsCounter = 0;
    private static int getCallsCounter = 0;

    private static final HashMap<String, List<String>> SHARED_CACHE = new HashMap<String, List<String>> (){

        @Override
        public boolean containsKey (Object o) {
            containsKeyCallsCounter++;
            return super.containsKey (o);
        }

        @Override
        public List<String> get (Object o) {
            getCallsCounter++;
            return super.get (o);
        }
    };

    @Test
    public void curlCommandMatchesShouldBeSavedInCache () {
        setInternalState (ReadArguments.class, "CACHED_ARGS_MATCHES", SHARED_CACHE);

        String args = "-X POST -H 'Content-Type:application/json' " +
                "-d '{\"test\":{\"name\":\"TEST_NAME\",\"value\":\"TEST_VALUE\"}}'";

        CommandLine result1 = ReadArguments.getCommandLineFromRequest (args,
                emptyList());

        assertEquals (1, containsKeyCallsCounter);
        assertEquals (0, getCallsCounter);

        CommandLine result2 = ReadArguments.getCommandLineFromRequest (args,
                emptyList());

        assertEquals (2, containsKeyCallsCounter);
        assertEquals (1, getCallsCounter);

        assertArrayEquals (result1.getArgs (), result2.getArgs ());

        assertEquals (1, SHARED_CACHE.size ());
        assertTrue (SHARED_CACHE.containsKey (args + " "));
        assertEquals (asList ("-X", "POST", "-H", "'Content-Type:application/json'", "-d", "'{\"test\":{\"name" +
                "\":\"TEST_NAME\",\"value\":\"TEST_VALUE\"}}'"), SHARED_CACHE.get (args + " "));
    }
}
