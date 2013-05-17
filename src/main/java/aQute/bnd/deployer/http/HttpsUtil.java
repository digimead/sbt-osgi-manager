/**
 * Copy of the code from github.com/bndtools/bnd, reason: not available at Maven central or other repository
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aQute.bnd.deployer.http;

import java.net.*;
import java.security.*;
import java.security.cert.*;

import javax.net.ssl.*;

public class HttpsUtil {

	public static final String	PROP_DISABLE_SERVER_CERT_VERIFY	= "disableServerVerify";

	static void disableServerVerification(URLConnection connection) throws GeneralSecurityException {
		if (!(connection instanceof HttpsURLConnection))
			return;

		HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}

				public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
			}
		};

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustAllCerts, new SecureRandom());

		SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		httpsConnection.setSSLSocketFactory(sslSocketFactory);

		HostnameVerifier trustAnyHost = new HostnameVerifier() {
			public boolean verify(String string, SSLSession session) {
				return true;
			}
		};
		httpsConnection.setHostnameVerifier(trustAnyHost);
	}
}