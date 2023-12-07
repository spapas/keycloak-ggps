package gr.hcg.spapas.ggps;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Serafeim Papastefanos
 */
public class GgpsProviderTest {

	@BeforeEach
	public void beforeEach() {

	}


	@Test
	public void testFindUser() {
		String s = "<root><userinfo userid=\"User068933130   \" taxid=\"068933130   \" lastname=\"ΛΝ\" firstname=\"ΦΝ\" fathername=\"ΦΑΝ\" mothername=\"ΜΑΝ\" birthyear=\"1950\"/></root>";
		JsonNode jn = GgpsXmlToJson.toJsonNode(s);
		assertEquals(jn.get("taxid").asText(), "068933130");
		assertEquals(jn.get("lastName").asText(), "ΛΝ");

	}

}
