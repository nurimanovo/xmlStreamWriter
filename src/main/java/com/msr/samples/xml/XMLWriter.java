package com.msr.samples.xml;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class XMLWriter implements AutoCloseable {
    private XMLStreamWriter xmlStreamWriter;
    private Marshaller marshaller;
    private OutputStream outputStream;
    private int openTagsCount = 0;

    public static XMLWriter writer(OutputStream outputStream, QName rootElement, Class... classes)
            throws JAXBException, XMLStreamException {
        return new XMLWriter(outputStream, classes, rootElement);
    }

    /**
     * Инициализация XMLWriter
     *
     * @param os          OutputStream для сериализации
     * @param classes     - набор классов для работы (все классы, по которым JAXB &amp; marshaller будут создавать xml)
     * @param rootElement rootElement
     */
    private XMLWriter(OutputStream os, Class[] classes, QName rootElement)
            throws JAXBException, XMLStreamException {
        this.outputStream = new BufferedOutputStream(os);

        marshaller = JAXBContext.newInstance(classes).createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
//        xmlOutputFactory.setProperty("javax.xml.stream.isRepairingNamespaces", Boolean.TRUE);
        xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(this.outputStream, StandardCharsets.UTF_8.name());
        xmlStreamWriter.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        beginXMLTag(rootElement);
        xmlStreamWriter.writeNamespace(rootElement.getPrefix(), rootElement.getNamespaceURI());
    }

    /**
     * @param element     - объект по которому marshaller будет строить xml
     * @param elementName - название начального/конечного тега внутри которого будет строится xml
     */
    public XMLWriter element(Object element, QName elementName) throws JAXBException {
        if (element == null)
            throw new IllegalArgumentException("Parameter [element] not defined");

        if (elementName == null)
            throw new IllegalArgumentException("Parameter [elementName] not defined");

        JAXBElement<Object> jaxbElement = new JAXBElement<>(elementName, (Class<Object>) element.getClass(), element);

        marshaller.marshal(jaxbElement, xmlStreamWriter);

        return this;
    }

    public XMLWriter beginXMLTag(QName tag) throws XMLStreamException {
        if (tag.getNamespaceURI().equals(XMLConstants.NULL_NS_URI)) {
            xmlStreamWriter.writeStartElement(tag.getLocalPart());
        } else {
            String existPrefix = xmlStreamWriter.getPrefix(tag.getNamespaceURI());
            xmlStreamWriter.writeStartElement(existPrefix != null ? existPrefix : tag.getPrefix(), tag.getLocalPart(), tag.getNamespaceURI());
        }
        openTagsCount++;
        return this;
    }

    private void endXMLTag() throws XMLStreamException {
        if (openTagsCount > 0) {
            xmlStreamWriter.writeEndElement();
            openTagsCount--;
        }
    }

    /**
     * закрывает открытые теги / освобождает ресурсы
     * освобождает потоки методом close()
     */
    @Override
    public void close() throws XMLStreamException, IOException {
        while (openTagsCount > 0) {
            endXMLTag();
        }
        xmlStreamWriter.writeEndDocument();
        xmlStreamWriter.close();
        outputStream.close();
    }
}
