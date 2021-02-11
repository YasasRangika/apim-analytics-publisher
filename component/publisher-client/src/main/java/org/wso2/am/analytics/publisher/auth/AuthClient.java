/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.analytics.publisher.auth;

import feign.Feign;
import feign.FeignException;
import feign.RetryableException;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;
import org.wso2.am.analytics.publisher.exception.ConnectionRecoverableException;
import org.wso2.am.analytics.publisher.exception.ConnectionUnrecoverableException;

/**
 * Auth client to generate SAS token that can use to authenticate with event hub
 */
public class AuthClient {
    public static final String AUTH_HEADER = "Authorization";

    public static String getSASToken(String authEndpoint, String token) throws ConnectionRecoverableException,
                                                                               ConnectionUnrecoverableException {

        DefaultApi defaultApi = Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger())
                .requestInterceptor(requestTemplate -> requestTemplate.header(AUTH_HEADER, "Bearer " + token))
                .target(DefaultApi.class, authEndpoint);
        try {
            TokenDetailsDTO dto = defaultApi.tokenGet();
            return dto.getToken();
        } catch (FeignException.Unauthorized e) {
            throw new ConnectionUnrecoverableException(
                    "Invalid user token. Analytics data publishing will be disabled. Please update apim.analytics"
                            + ".auth_token in configuration and restart the instance", e);
        } catch (RetryableException e) {
            throw new ConnectionRecoverableException("Provided authentication endpoint " + authEndpoint + " is not "
                                                             + "reachable.");
        } catch (IllegalArgumentException e) {
            throw new ConnectionUnrecoverableException("Invalid apim.analytics configurations provided. Analytics "
                                                               + "data publishing will be disabled. Please update "
                                                               + "configurations and restart the instance.");
        } catch (Exception e) {
            //we will retry for any other exception
            throw new ConnectionRecoverableException("Exception " + e.getClass() + " occurred.");
        }
    }
}
