/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.platform.web.common.locale;

import java.util.Locale;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Provides locale and timezone.
 *
 * @since 5.6
 */
public interface LocaleProvider {

    /**
     * @return the Locale to be used or null to let the caller decides.
     */
    public Locale getLocale(CoreSession session);

    /**
     * Gets the locale stored in the given user profile.
     *
     * @return the Locale to be used or null to let the caller decide
     */
    Locale getLocale(DocumentModel userProfileDoc);

    /**
     * @return the Locale to be used or a default Locale
     * @since 8.2
     */
    public Locale getLocaleWithDefault(CoreSession session);

    /**
     * @return the Locale stored in userProfile or a default Locale
     * @since 8.2
     */
    public Locale getLocaleWithDefault(DocumentModel userProfileDoc);

    /**
     * @return the Timezone to be used or null to let the caller decides.
     */
    public TimeZone getTimeZone(CoreSession session);

}
