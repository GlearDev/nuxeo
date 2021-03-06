/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.Environment.DISTRIBUTION_PACKAGE;
import static org.nuxeo.common.Environment.DISTRIBUTION_VERSION;
import static org.nuxeo.common.Environment.PRODUCT_VERSION;

import java.net.URL;
import java.util.Properties;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginVideo;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.sun.jersey.api.uri.UriComponent;

public class TestLoginScreenConfig extends NXRuntimeTestCase {

    private static final String WEB_BUNDLE = "org.nuxeo.ecm.platform.web.common";

    private static final String WEB_BUNDLE_TEST = "org.nuxeo.ecm.platform.web.common.test";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-framework.xml");
        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-contrib.xml");
        deployContrib(WEB_BUNDLE_TEST, "OSGI-INF/test-loginscreenconfig.xml");

        Properties properties = Framework.getProperties();
        properties.setProperty(PRODUCT_VERSION, "LTS-2015");
        properties.setProperty(DISTRIBUTION_VERSION, "7.10");
        properties.setProperty(DISTRIBUTION_PACKAGE, "zip");

    }

    private PluggableAuthenticationService getAuthService() {
        PluggableAuthenticationService authService;
        authService = (PluggableAuthenticationService) Framework.getRuntime()
                                                                .getComponent(PluggableAuthenticationService.NAME);

        return authService;
    }

    @Test
    public void testSimpleConfig() {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        assertEquals("#CCCCCC", config.getHeaderStyle());
        assertNull(config.getDisableBackgroundSizeCover());
        assertEquals(3, config.getProviders().size());
        assertTrue(config.hasVideos());
        assertEquals(2, config.getVideos().size());

        LoginVideo loginVideo = config.getVideos().get(0);
        assertTrue(isNotBlank(loginVideo.getType()));
        assertTrue(isNotBlank(loginVideo.getSrc()));
    }

    @Test
    public void testVariableExpension() {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        String style = config.getBodyBackgroundStyle();
        assertTrue(style.contains("/nuxeo/img/login_bg.png"));
    }

    @Test
    public void testMergeConfig() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        assertEquals("#CCCCCC", config.getHeaderStyle());
        assertEquals("Something", config.getFooterStyle());
        assertEquals(3, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("facebook"));
        assertNotNull(config.getProvider("linkedin"));
        assertTrue(config.getDisplayNews());
        assertNull(config.getDisableBackgroundSizeCover());

        assertEquals("XXXX", config.getProvider("google").getLink(null, null));
        deployContrib(WEB_BUNDLE_TEST, "OSGI-INF/test-loginscreenconfig-merge.xml");

        config = authService.getLoginScreenConfig();
        assertNotNull(config);

        assertEquals("#DDDDDD", config.getHeaderStyle());
        assertEquals("Something", config.getFooterStyle());
        assertFalse(config.getDisplayNews());
        assertEquals(2, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("linkedin"));
        assertNull(config.getProvider("facebook"));
        assertEquals("News", config.getProvider("google").getLink(null, null));
        assertEquals(Boolean.TRUE, config.getDisableBackgroundSizeCover());

        assertFalse(config.hasVideos());

        assertTrue(config.getVideoMuted());
        assertFalse(config.getVideoLoop());
    }

    @Test
    public void testUndeployConfig() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        undeployContrib(WEB_BUNDLE_TEST, "OSGI-INF/test-loginscreenconfig.xml");

        config = authService.getLoginScreenConfig();
        assertNull(config);
    }

    @Test
    public void testHelper() throws Exception {

        LoginScreenConfig config = LoginScreenHelper.getConfig();
        assertNotNull(config);

        assertEquals("#CCCCCC", config.getHeaderStyle());
        assertEquals("Something", config.getFooterStyle());
        assertEquals(3, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("facebook"));
        assertNotNull(config.getProvider("linkedin"));
        assertEquals("XXXX", config.getProvider("google").getLink(null, null));

        LoginScreenHelper.registerLoginProvider("google", "XXX", "new", null, null, null);
        LoginScreenHelper.registerLoginProvider("OuvertId", "AAA", "BBB", null, null, null);

        assertEquals(4, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("linkedin"));
        assertNotNull(config.getProvider("facebook"));
        assertNotNull(config.getProvider("OuvertId"));
        assertEquals("new", config.getProvider("google").getLink(null, null));
        assertEquals("BBB", config.getProvider("OuvertId").getLink(null, null));

    }

    @Test
    public void iframe_url_embeds_the_distribution_package_type_and_version() throws Exception {

        LoginScreenConfig config = new LoginScreenConfig();
        String strUrl = config.getNewsIframeUrl();
        if (!strUrl.startsWith("http")) {
            strUrl = "http:" + strUrl;
        }
        URL url = new URL(strUrl);

        MultivaluedMap<String, String> query = UriComponent.decodeQuery(url.getQuery(), true);

        assertThat(query.keySet()).contains(PRODUCT_VERSION, DISTRIBUTION_VERSION, DISTRIBUTION_PACKAGE);
        assertThat(query.get(PRODUCT_VERSION)).contains("LTS-2015");
        assertThat(query.get(DISTRIBUTION_VERSION)).contains("7.10");
        assertThat(query.get(DISTRIBUTION_PACKAGE)).contains("zip");

    }
}
