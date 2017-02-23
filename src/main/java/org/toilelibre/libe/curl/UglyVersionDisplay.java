package org.toilelibre.libe.curl;

import static org.toilelibre.libe.curl.Version.BUILD_TIME;
import static org.toilelibre.libe.curl.Version.VERSION;

class UglyVersionDisplay {


    static void stopAndDisplayVersionIfThe (boolean isTrue) {
        if (!isTrue) {
            return;
        }

        System.out.println(Curl.class.getPackage().getName() + " version " + VERSION + ", build-time : " + BUILD_TIME);

        throw new Curl.CurlException(
                new IllegalArgumentException(
                        "You asked me to display the version. Probably not a production-ready code"));
    }
}
