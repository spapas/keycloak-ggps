package gr.hcg.spapas.ggps;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
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
import org.jboss.logging.Logger;

public class GgpsIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {
    private static final Logger logger = Logger.getLogger(GgpsIdentityProvider.class);
    public static final String DEFAULT_BASE_URL = "https://test.gsis.gr/oauth2server";
    public static final String AUTH_FRAGMENT = "/oauth/authorize";
    public static final String TOKEN_FRAGMENT = "/oauth/token";
    public static final String LOGOUT_FRAGMENT = "/logout/";
    public static final String PROFILE_FRAGMENT = "/userinfo?format=xml";
    protected static final String BASE_URL_KEY = "baseUrl";

    private final String authUrl;
    private final String tokenUrl;
    private final String profileUrl;
    private final String logoutUrl;

    public static void logDebug(String logging) {
        logger.debug("\u001B[34m" + logging + "\u001B[0m");
    }
    public static void logInfo(String logging) {
        logger.info("\u001B[32m" + logging + "\u001B[0m");
    }

    public static void logError(String logging) {
        logger.error("\u001B[31m" + logging + "\u001B[0m");
    }
    private final String clientId;
    public GgpsIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);
        String baseUrl = getUrlFromConfig(config, BASE_URL_KEY, DEFAULT_BASE_URL);
        authUrl = baseUrl + AUTH_FRAGMENT;
        tokenUrl = baseUrl + TOKEN_FRAGMENT;
        profileUrl = baseUrl + PROFILE_FRAGMENT;
        logoutUrl = baseUrl + LOGOUT_FRAGMENT;
        clientId = config.getClientId();

        config.setAuthorizationUrl(authUrl);
        config.setTokenUrl(tokenUrl);
        config.setUserInfoUrl(profileUrl);
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

        BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "taxid"), getConfig());

		String username = getJsonProperty(profile, "userid");
		user.setUsername(username);
		user.setFirstName(getJsonProperty(profile, "firstName"));
		user.setLastName(getJsonProperty(profile, "lastName"));

        user.setUserAttribute("fatherName", getJsonProperty(profile, "fatherName"));
        user.setUserAttribute("motherName", getJsonProperty(profile, "motherName"));
        user.setUserAttribute("birthYear", getJsonProperty(profile, "birthYear"));
        user.setUserAttribute("taxid", getJsonProperty(profile, "taxid"));
        user.setUserAttribute("userid", getJsonProperty(profile, "userid"));
		// user.setIdpConfig();
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
        logInfo("Getting federeted identity with accessToken " + accessToken);
		try (SimpleHttp.Response response = SimpleHttp.doGet(profileUrl, session)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "text/xml")
                        .asResponse()) {

                    if (Response.Status.fromStatusCode(response.getStatus()).getFamily() != Response.Status.Family.SUCCESSFUL) {
                        logger.warnf("Profile endpoint returned an error (%d): %s", response.getStatus(), response.asString());
                        throw new IdentityBrokerException("Profile could not be retrieved from the github endpoint");
                    }

                    JsonNode profile = GgpsXmlToJson.toJsonNode(response.asString());
                    logDebug("profile retrieved from ggps: " + profile);

                    BrokeredIdentityContext user = extractIdentityFromProfile(null, profile);

                    return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Profile could not be retrieved from the GGPS endpoint", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
        return "read";
	}

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        logger.info("Logging out ... client id = " + clientId +" uri info " + uriInfo.getQueryParameters());
        String postLogoutRedirect = uriInfo.getQueryParameters().getFirst("post_logout_redirect_uri");
        logger.info("Post logout = " + postLogoutRedirect);
        UriBuilder logoutUri = UriBuilder.fromUri(logoutUrl + clientId).queryParam("url", postLogoutRedirect);
        logger.info("Full logout URL = " + logoutUri.build());
        String userId = userSession.getUser().getId();
        session.sessions().getUserSession(realm, userId);
        AuthenticationManager.backchannelLogout(session, userSession, false);

        Response response = Response.status(302).location(logoutUri.build()).build();
        return response;

    }

}
