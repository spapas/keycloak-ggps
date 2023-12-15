

package gr.hcg.spapas.ggps;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import java.util.Iterator;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.plugins.providers.jaxb.XmlNamespacePrefixMapper;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

public class GgpsIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    public static final String DEFAULT_BASE_URL = "https://test.gsis.gr/oauth2server";
    public static final String AUTH_FRAGMENT = "/oauth/authorize";
    public static final String TOKEN_FRAGMENT = "/oauth/token";
    public static final String DEFAULT_AUTH_URL = DEFAULT_BASE_URL + AUTH_FRAGMENT;
    public static final String DEFAULT_TOKEN_URL = DEFAULT_BASE_URL + TOKEN_FRAGMENT;
    /** @deprecated Use {@link #DEFAULT_AUTH_URL} instead. */
    @Deprecated
    public static final String AUTH_URL = DEFAULT_AUTH_URL;
    /** @deprecated Use {@link #DEFAULT_TOKEN_URL} instead. */
    @Deprecated
    public static final String TOKEN_URL = DEFAULT_TOKEN_URL;

    public static final String DEFAULT_API_URL = "https://test.gsis.gr/oauth2server";

    public static final String DEFAULT_LOGOUT_URL = "https://test.gsis.gr/oauth2server/logout/";
    public static final String PROFILE_FRAGMENT = "/userinfo?format=xml";
    public static final String DEFAULT_PROFILE_URL = DEFAULT_API_URL + PROFILE_FRAGMENT;
    /** @deprecated Use {@link #DEFAULT_PROFILE_URL} instead. */
    @Deprecated
    public static final String PROFILE_URL = DEFAULT_PROFILE_URL;

    /** Base URL key in config map. */
    protected static final String BASE_URL_KEY = "baseUrl";
    /** API URL key in config map. */
    protected static final String API_URL_KEY = "apiUrl";
    /** Email URL key in config map. */
    protected static final String EMAIL_URL_KEY = "emailUrl";

    private final String authUrl;
    private final String tokenUrl;
    private final String profileUrl;

    private final String clientId;
    public GgpsIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);

        String baseUrl = getUrlFromConfig(config, BASE_URL_KEY, DEFAULT_BASE_URL);
        String apiUrl = getUrlFromConfig(config, API_URL_KEY, DEFAULT_API_URL);

        authUrl = baseUrl + AUTH_FRAGMENT;
        tokenUrl = baseUrl + TOKEN_FRAGMENT;
        profileUrl = apiUrl + PROFILE_FRAGMENT;
        clientId = config.getClientId();

        config.setAuthorizationUrl(authUrl);
        config.setTokenUrl(tokenUrl);
        config.setUserInfoUrl(profileUrl);
        //config.getConfig().put(EMAIL_URL_KEY, emailUrl);
    }

    /**
     * Get URL from config with default value fallback.
     *
     * @param config Identity provider configuration.
     * @param key Key to look for value in config's config map.
     * @param defaultValue Default value if value at key is null or empty string.
     * @return URL for specified key in the configuration with default value fallback.
     */
    protected static String getUrlFromConfig(OAuth2IdentityProviderConfig config, String key, String defaultValue) {
        String url = config.getConfig().get(key);
        if (url == null || url.trim().isEmpty()) {
            url = defaultValue;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return profileUrl;
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {

        BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "taxid"));

		String username = getJsonProperty(profile, "userid");
		user.setUsername(username);
		user.setFirstName(getJsonProperty(profile, "firstName"));
		user.setLastName(getJsonProperty(profile, "lastName"));

        user.setUserAttribute("fatherName", getJsonProperty(profile, "fatherName"));
        user.setUserAttribute("motherName", getJsonProperty(profile, "motherName"));
        user.setUserAttribute("birthYear", getJsonProperty(profile, "birthYear"));
        user.setUserAttribute("taxid", getJsonProperty(profile, "taxid"));
        user.setUserAttribute("userid", getJsonProperty(profile, "userid"));
		user.setIdpConfig(getConfig());
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        user.setSingleAttribute("fatherName", context.getUserAttribute("fatherName"));
        user.setSingleAttribute("motherName", context.getUserAttribute("motherName"));
        user.setSingleAttribute("birthYear", context.getUserAttribute("birthYear"));
        user.setSingleAttribute("taxid", context.getUserAttribute("taxid"));
        user.setSingleAttribute("userid", context.getUserAttribute("userid"));
    }

	@Override
	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try (SimpleHttp.Response response = SimpleHttp.doGet(profileUrl, session)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "text/xml")
                        .asResponse()) {

                    if (Response.Status.fromStatusCode(response.getStatus()).getFamily() != Response.Status.Family.SUCCESSFUL) {
                        logger.warnf("Profile endpoint returned an error (%d): %s", response.getStatus(), response.asString());
                        throw new IdentityBrokerException("Profile could not be retrieved from the github endpoint");
                    }

                    JsonNode profile = GgpsXmlToJson.toJsonNode(response.asString());
                    logger.tracef("profile retrieved from github: %s", profile);

                    BrokeredIdentityContext user = extractIdentityFromProfile(null, profile);

                    return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Profile could not be retrieved from the github endpoint", e);
		}
	}

    /*
	private String searchEmail(String accessToken) {
		try (SimpleHttp.Response response = SimpleHttp.doGet(emailUrl, session)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/json")
                        .asResponse()) {

                    if (Response.Status.fromStatusCode(response.getStatus()).getFamily() != Response.Status.Family.SUCCESSFUL) {
                        logger.warnf("Primary email endpoint returned an error (%d): %s", response.getStatus(), response.asString());
                        throw new IdentityBrokerException("Primary email could not be retrieved from the github endpoint");
                    }

                    JsonNode emails = response.asJson();
                    logger.tracef("emails retrieved from github: %s", emails);
                    if (emails.isArray()) {
                        Iterator<JsonNode> loop = emails.elements();
                        while (loop.hasNext()) {
                            JsonNode mail = loop.next();
                            JsonNode primary = mail.get("primary");
                            if (primary != null && primary.asBoolean()) {
                                return getJsonProperty(mail, "email");
                            }
                        }
                    }

                    throw new IdentityBrokerException("Primary email from github is not found in the user's email list.");
		} catch (Exception e) {
			throw new IdentityBrokerException("Primary email could not be retrieved from the github endpoint", e);
		}
	}
    */
	@Override
	protected String getDefaultScopes() {
        return "read";
	}

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        logger.info("Logging out ... client id = " + clientId);

        UriBuilder logoutUri = UriBuilder.fromUri(DEFAULT_LOGOUT_URL + clientId + "?url=https://kc.hcg.gr/realms/ggps-uat/protocol/openid-connect/logout");
//                .queryParam("client_id", );
        logger.info("URL = " + logoutUri);

        Response response = Response.status(302).location(logoutUri.build()).build();
        return response;
        /*
        if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().equals("")) return null;
        String idToken = userSession.getNote(FEDERATED_ID_TOKEN);
        if (idToken != null && getConfig().isBackchannelSupported()) {
            backchannelLogout(userSession, idToken);
            return null;
        } else {
            String sessionId = userSession.getId();
            UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
                    .queryParam("state", sessionId);
            if (idToken != null) logoutUri.queryParam("id_token_hint", idToken);
            String redirect = RealmsResource.brokerUrl(uriInfo)
                    .path(IdentityBrokerService.class, "getEndpoint")
                    .path(OIDCEndpoint.class, "logoutResponse")
                    .build(realm.getName(), getConfig().getAlias()).toString();
            logoutUri.queryParam("post_logout_redirect_uri", redirect);
            Response response = Response.status(302).location(logoutUri.build()).build();
            return response;
        }

         */
    }

}
