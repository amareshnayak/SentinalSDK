/**
 * Copyright (c) 2016 OpenCloud
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   1. Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. The name of the author may not be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 *   4. The source code may not be used to create, develop, use or distribute
 *      software for use on any platform other than the OpenCloud Rhino
 *      platform or any successor products.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Visit OpenCloud Developer's Portal for how-to guides, examples,
 * documentation, forums and more: http://developer.opencloud.com
 */
package com.opencloud.slee.services.easysip.common;

import org.jainslee.resources.sip.SipURI;
import org.jainslee.resources.sip.URI;
import org.jainslee.resources.sip.TelURL;
import org.jainslee.resources.sip.SipMessage;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

public class EasySipUtils {

    public static String getCanonicalAddress(URI uri) {
        StringBuffer sb = new StringBuffer();
        if (uri.isSipURI()) {
            SipURI sipuri = (SipURI) uri;
            sb.append(sipuri.getScheme()).append(':');
            sb.append(sipuri.getUser()).append('@').append(sipuri.getHost());
            // Only put port in if we need to (ie. it is non-standard).
            // So sip:fred@abc.com:5060 becomes sip:fred@abc.com,
            // and sips:bob@abc.com:5061 becomes sips:bob@abc.com.
            int port = sipuri.getPort();
            if (port != -1) {
                if (sipuri.isSecure() && port != 5061) sb.append(':').append(port);
                else if (port != 5060) sb.append(':').append(port);
            }
        }
        else {
            TelURL tel = (TelURL) uri;
            sb.append("tel:").append(tel.getPhoneNumber());
        }
        return sb.toString();
    }

    public static String toSipDate(Calendar cal) {
        return formatter.format(cal.getTime());
    }

    public static String toSipDate(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return toSipDate(cal);
    }

    public static int getSequenceNumber(SipMessage message) {
        String cseqHeader = message.getHeader("CSeq");
        // ALL messages MUST have a valid CSeq header, so no error checking here.
        return Integer.parseInt(new StringTokenizer(cseqHeader).nextToken());
    }

    public static boolean isInDomain(String host, String domain) {
        host = host.toLowerCase();
        domain = domain.toLowerCase();
        if (host.endsWith(domain)) {
            // Maybe a match - check if it is the same name or hostname has a '.' before the domain name
            return host.length() == domain.length() ||
                    host.charAt(host.length() - domain.length() - 1) == '.';
        }
        return false;
    }

    public static boolean isIPAddress(String s) {
        return IPV4_PATTERN.matcher(s).matches() || IPV6_PATTERN.matcher(s).matches();
    }

    public static final String SIP_DATE_FORMAT = "EEE, dd MMM yyyy hh:mm:ss z";
    public static final String SIP_TIMEZONE = "GMT";

    private static Pattern IPV4_PATTERN = Pattern.compile("(\\d{1,3}\\.){3}\\d{1,3}");
    private static Pattern IPV6_PATTERN = Pattern.compile("\\[[0-9a-f\\:]+\\]", Pattern.CASE_INSENSITIVE);

    private static final SimpleDateFormat formatter = new SimpleDateFormat(SIP_DATE_FORMAT, Locale.ENGLISH);
    static {
        formatter.setTimeZone(TimeZone.getTimeZone(SIP_TIMEZONE));
    }
}
