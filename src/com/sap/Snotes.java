//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.01.18 at 08:20:52 PM GMT+03:00 
//


package com.sap;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for JaxbSnotes complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="JaxbSnotes">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="n" type="{}JaxbNote" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "JaxbSnotes", namespace = "", propOrder = {
    "ns"
})
@XmlRootElement(name = "snotes", namespace = "")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class Snotes {

    @XmlElement(name = "n", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<JaxbNote> ns;

    /**
     * Gets the value of the ns property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ns property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNS().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JaxbNote }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<JaxbNote> getNS() {
        if (ns == null) {
            ns = new ArrayList<JaxbNote>();
        }
        return this.ns;
    }

}