
package gr.hcg.spapas.ggps;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class GgpsIdentityProviderFactory extends AbstractIdentityProviderFactory<GgpsIdentityProvider> implements SocialIdentityProviderFactory<GgpsIdentityProvider> {

    public static final String PROVIDER_ID = "ggps";

    @Override
    public String getName() {
        return "Ggps";
    }

    @Override
    public GgpsIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new GgpsIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
    }

    @Override
    public OAuth2IdentityProviderConfig createConfig() {
        return new OAuth2IdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create().property()
                .name("baseUrl").label("Base URL").helpText("Override the default Base URL for this identity provider.")
                .type(ProviderConfigProperty.STRING_TYPE).add().property()
                .name("apiUrl").label("API URL").helpText("Override the default API URL for this identity provider.")
                .type(ProviderConfigProperty.STRING_TYPE).add().build();
    }
}
