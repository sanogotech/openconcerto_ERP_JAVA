package org.openconcerto.modules.finance.payment.ebics.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.TransformSpi;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.openconcerto.utils.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class TransformXPointer
 */
public class TransformXPointer extends TransformSpi {

    /** Field implementedTransformURI */
    public static final String implementedTransformURI = Transforms.TRANSFORM_XPOINTER;

    /** @inheritDoc */
    protected String engineGetURI() {
        return implementedTransformURI;
    }

    /**
     * Method enginePerformTransform
     * 
     * @param input
     * @return {@link XMLSignatureInput} as the result of transformation
     * @throws TransformationException
     * 
     */
    protected XMLSignatureInput enginePerformTransform(XMLSignatureInput input, Transform _transformObject) throws TransformationException {
        //
        try {
            // System.err.println(new String(input.getBytes()));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(input.getBytes()));

            String PATH = "//*[@authenticate='true']";
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile(PATH);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (nodes.getLength() < 1) {
                System.out.println("Invalid document, can't find node by PATH: " + PATH);
                return null;
            }
            final Set<Node> l = new HashSet<Node>();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node item = nodes.item(i);
                l.add(item);
                final NodeList childNodes = item.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node node = childNodes.item(i);
                    // l.add(node);
                }
            }
            XMLSignatureInput out = new XMLSignatureInput(nodes.item(0));
            // System.err.println(new String(out.getBytes()));
            return out;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Object exArgs[] = { implementedTransformURI };

        throw new TransformationException("signature.Transform.NotYetImplemented", exArgs);
    }
}