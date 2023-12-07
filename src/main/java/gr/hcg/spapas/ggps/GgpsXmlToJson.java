package gr.hcg.spapas.ggps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GgpsXmlToJson {
    public static JsonNode toJsonNode(String s) {
        try {
            // Create a DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Create a DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse the XML string to create a Document
            Document document = builder.parse(new java.io.ByteArrayInputStream(s.getBytes()));

            // Normalize the document
            document.getDocumentElement().normalize();

            // Get the root element
            Element root = document.getDocumentElement();

            // Get the 'userinfo' element
            NodeList userInfoList = root.getElementsByTagName("userinfo");
            Node userInfoNode = userInfoList.item(0);

            // Check if 'userinfo' element exists
            if (userInfoNode.getNodeType() == Node.ELEMENT_NODE) {
                Element userInfoElement = (Element) userInfoNode;

                // Get userinfo attributes
                String userid = userInfoElement.getAttribute("userid").trim();
                String taxid = userInfoElement.getAttribute("taxid").trim();
                String lastname = userInfoElement.getAttribute("lastname").trim();
                String firstname = userInfoElement.getAttribute("firstname").trim();
                String fathername = userInfoElement.getAttribute("fathername").trim();
                String mothername = userInfoElement.getAttribute("mothername").trim();
                String birthyear = userInfoElement.getAttribute("birthyear").trim();

                // Print extracted values
                System.out.println("userid: " + userid);
                System.out.println("taxid: " + taxid);
                System.out.println("lastname: " + lastname);
                System.out.println("firstname: " + firstname);
                System.out.println("fathername: " + fathername);
                System.out.println("mothername: " + mothername);
                System.out.println("birthyear: " + birthyear);

                HashMap hashMap = new HashMap<String, String>();
                hashMap.put("userid", userid);
                hashMap.put("taxid", taxid);
                hashMap.put("lastName", lastname);
                hashMap.put("firstName", firstname);
                hashMap.put("fatherName", fathername);
                hashMap.put("motherName", mothername);
                hashMap.put("birthYear", birthyear);
                ObjectMapper objectMapper = new ObjectMapper();

                String jsonString = objectMapper.writeValueAsString(hashMap);
                System.out.println(jsonString);
                JsonNode jsonNode = objectMapper.readTree(jsonString);

                // Print the JsonNode
                System.out.println("JsonNode from HashMap:");
                System.out.println(jsonNode);
                return jsonNode;

            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

}