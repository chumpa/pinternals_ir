//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.01.18 at 08:20:52 PM GMT+03:00 
//


package com.sap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}SapNotesNumber"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}SapNotesKey"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Title"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Type"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Category"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}ComponentKey"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}ComponentText"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Language"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Priority"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}ReleasedOn"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Version"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}CurrentLang"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Lang"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}LangMaster"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}LangText"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}TypeKey"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}TypeText"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Text"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}RefNum"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}RefType"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}RefTitle"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}RefUrl"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}RefKey"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}LanguageText"/>
 *         &lt;element ref="{http://schemas.microsoft.com/ado/2007/08/dataservices}Favorite"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "properties", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class Properties {

    @XmlElement(name = "SapNotesNumber", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String sapNotesNumber;
    @XmlElement(name = "SapNotesKey", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String sapNotesKey;
    @XmlElement(name = "Title", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String title;
    @XmlElement(name = "Type", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String type;
    @XmlElement(name = "Category", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String category;
    @XmlElement(name = "ComponentKey", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String componentKey;
    @XmlElement(name = "ComponentText", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String componentText;
    @XmlElement(name = "Language", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String language;
    @XmlElement(name = "Priority", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String priority;
    @XmlElement(name = "ReleasedOn", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String releasedOn;
    @XmlElement(name = "Version", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String version;
    @XmlElement(name = "CurrentLang", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String currentLang;
    @XmlElement(name = "Lang", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String lang;
    @XmlElement(name = "LangMaster", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String langMaster;
    @XmlElement(name = "LangText", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String langText;
    @XmlElement(name = "TypeKey", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String typeKey;
    @XmlElement(name = "TypeText", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String typeText;
    @XmlElement(name = "Text", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String text;
    @XmlElement(name = "RefNum", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String refNum;
    @XmlElement(name = "RefType", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String refType;
    @XmlElement(name = "RefTitle", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String refTitle;
    @XmlElement(name = "RefUrl", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String refUrl;
    @XmlElement(name = "RefKey", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String refKey;
    @XmlElement(name = "LanguageText", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String languageText;
    @XmlElement(name = "Favorite", namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String favorite;

    /**
     * Gets the value of the sapNotesNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getSapNotesNumber() {
        return sapNotesNumber;
    }

    /**
     * Sets the value of the sapNotesNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setSapNotesNumber(String value) {
        this.sapNotesNumber = value;
    }

    /**
     * Gets the value of the sapNotesKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getSapNotesKey() {
        return sapNotesKey;
    }

    /**
     * Sets the value of the sapNotesKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setSapNotesKey(String value) {
        this.sapNotesKey = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the category property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setCategory(String value) {
        this.category = value;
    }

    /**
     * Gets the value of the componentKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getComponentKey() {
        return componentKey;
    }

    /**
     * Sets the value of the componentKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setComponentKey(String value) {
        this.componentKey = value;
    }

    /**
     * Gets the value of the componentText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getComponentText() {
        return componentText;
    }

    /**
     * Sets the value of the componentText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setComponentText(String value) {
        this.componentText = value;
    }

    /**
     * Gets the value of the language property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the value of the language property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setLanguage(String value) {
        this.language = value;
    }

    /**
     * Gets the value of the priority property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getPriority() {
        return priority;
    }

    /**
     * Sets the value of the priority property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setPriority(String value) {
        this.priority = value;
    }

    /**
     * Gets the value of the releasedOn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getReleasedOn() {
        return releasedOn;
    }

    /**
     * Sets the value of the releasedOn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setReleasedOn(String value) {
        this.releasedOn = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the currentLang property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getCurrentLang() {
        return currentLang;
    }

    /**
     * Sets the value of the currentLang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setCurrentLang(String value) {
        this.currentLang = value;
    }

    /**
     * Gets the value of the lang property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getLang() {
        return lang;
    }

    /**
     * Sets the value of the lang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setLang(String value) {
        this.lang = value;
    }

    /**
     * Gets the value of the langMaster property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getLangMaster() {
        return langMaster;
    }

    /**
     * Sets the value of the langMaster property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setLangMaster(String value) {
        this.langMaster = value;
    }

    /**
     * Gets the value of the langText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getLangText() {
        return langText;
    }

    /**
     * Sets the value of the langText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setLangText(String value) {
        this.langText = value;
    }

    /**
     * Gets the value of the typeKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getTypeKey() {
        return typeKey;
    }

    /**
     * Sets the value of the typeKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTypeKey(String value) {
        this.typeKey = value;
    }

    /**
     * Gets the value of the typeText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getTypeText() {
        return typeText;
    }

    /**
     * Sets the value of the typeText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTypeText(String value) {
        this.typeText = value;
    }

    /**
     * Gets the value of the text property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setText(String value) {
        this.text = value;
    }

    /**
     * Gets the value of the refNum property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getRefNum() {
        return refNum;
    }

    /**
     * Sets the value of the refNum property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRefNum(String value) {
        this.refNum = value;
    }

    /**
     * Gets the value of the refType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getRefType() {
        return refType;
    }

    /**
     * Sets the value of the refType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRefType(String value) {
        this.refType = value;
    }

    /**
     * Gets the value of the refTitle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getRefTitle() {
        return refTitle;
    }

    /**
     * Sets the value of the refTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRefTitle(String value) {
        this.refTitle = value;
    }

    /**
     * Gets the value of the refUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getRefUrl() {
        return refUrl;
    }

    /**
     * Sets the value of the refUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRefUrl(String value) {
        this.refUrl = value;
    }

    /**
     * Gets the value of the refKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getRefKey() {
        return refKey;
    }

    /**
     * Sets the value of the refKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRefKey(String value) {
        this.refKey = value;
    }

    /**
     * Gets the value of the languageText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getLanguageText() {
        return languageText;
    }

    /**
     * Sets the value of the languageText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setLanguageText(String value) {
        this.languageText = value;
    }

    /**
     * Gets the value of the favorite property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getFavorite() {
        return favorite;
    }

    /**
     * Sets the value of the favorite property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-01-18T08:20:52+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFavorite(String value) {
        this.favorite = value;
    }

}