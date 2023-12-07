
package gr.hcg.spapas.ggps;

import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;

/**
 * User attribute mapper.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class GgpsUserAttributeMapper extends AbstractJsonUserAttributeMapper {

	public static final String PROVIDER_ID = "ggps-user-attribute-mapper";
	private static final String[] cp = new String[] { GgpsIdentityProviderFactory.PROVIDER_ID };

	@Override
	public String[] getCompatibleProviders() {
		return cp;
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

}
