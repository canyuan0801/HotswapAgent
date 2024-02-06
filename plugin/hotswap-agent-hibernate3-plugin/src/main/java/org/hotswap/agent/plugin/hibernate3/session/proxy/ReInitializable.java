
package org.hotswap.agent.plugin.hibernate3.session.proxy;

import java.io.File;
import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;


public interface ReInitializable {


    void hotSwap();


    void reInitialize();


    OverrideConfig getOverrideConfig();


    Configuration setProperty(String propertyName, String value);


    Configuration configure(String resource) throws HibernateException;


    Configuration configure(URL url) throws HibernateException;


    Configuration configure(File configFile) throws HibernateException;


    Configuration configure(org.w3c.dom.Document document) throws HibernateException;


    Configuration configure() throws HibernateException;



    org.hibernate.SessionFactory _buildSessionFactory() throws org.hibernate.HibernateException;


    Configuration _setProperty(String propertyName, String value);


    Configuration _configure() throws HibernateException;


    Configuration _configure(String resource) throws HibernateException;


    Configuration _configure(URL url) throws HibernateException;


    Configuration _configure(File configFile) throws HibernateException;


    Configuration _configure(org.w3c.dom.Document document) throws HibernateException;
}
