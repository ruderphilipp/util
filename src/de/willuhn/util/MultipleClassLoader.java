/**********************************************************************
 * $Source: /cvsroot/jameica/util/src/de/willuhn/util/MultipleClassLoader.java,v $
 * $Revision: 1.15 $
 * $Date: 2004/04/01 19:02:02 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.util;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * ClassLoader der sich beliebiger anderer ClassLoader bedient.
 * @author willuhn
 * 05.01.2004
 */
public class MultipleClassLoader extends ClassLoader
{

  private ArrayList loaders 	= new ArrayList();
	private ArrayList urlList		= new ArrayList();
  private Hashtable cache 		= new Hashtable();
	private ClassFinder finder 	= new ClassFinder();
	
	boolean urlsChanged 				= false;
	private URL[] urls					= null;
	private URLClassLoader ucl	= null;
	
	private Logger logger = new Logger("MultipleClassLoader");
  
  
	/**
   * ct.
   */
  public MultipleClassLoader()
	{
	}

	/**
	 * Optionale Angabe eines Loggers.
	 * Wird er angegeben, schreibt der ClassLoader seine Infos da rein.
   * @param l zu verwendender ClassLoader.
   */
  public void setLogger(Logger l)
	{
		if (l == null)
			return;
		logger = l;
	}

  /**
   * Fuegt einen weiteren ClassLoader hinzu,
   * @param loader der hinzuzufuegende Classloader.
   */
  public void addClassloader(ClassLoader loader)
  {
  	if (loader == null)
  		return;
  	logger.debug("multipleClassLoader: adding class loader " + loader.getClass().getName());
    loaders.add(loader);
  }
  
  /**
   * Fuegt das uebergebene Jar-File oder Verzeichnis zum Class-Loader hinzu.
   * @param file das Jar-File oder Verzeichnis.
   * @throws MalformedURLException
   */
  public void add(File file) throws MalformedURLException
  {
  	if (file == null)
  		return;

		logger.debug("multipleClassLoader: adding file " + file.getAbsolutePath());

		urlList.add(file.toURL());
		urlsChanged = true;
  }

  /**
	 * Fuegt rekursiv alle Jar-Files zum Class-Loader hinzu, die sich im uebergebenen Verzeichnis befinden.
   * @param directory Verzeichnis mit Jar-Files.
   * @return eine Liste mit allen Jar-Files, die geladen wurden.
   * @throws MalformedURLException
   */
  public File[] addJars(File directory) throws MalformedURLException
	{
		// Liste aller Jars aus dem plugin-Verzeichnis holen
		FileFinder finder = new FileFinder(directory);
		finder.extension("jar");
		File[] jars = finder.findRecursive();

		if (jars == null || jars.length < 1)
		{
			return null;
		}

		for(int i=0;i<jars.length;++i)
		{
			urlList.add(jars[i].toURL());
			logger.debug("multipleClassLoader: adding file " + jars[i].getAbsolutePath());
		}
		urlsChanged = true;
		return jars;
	}

	/**
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	public InputStream getResourceAsStream(String name) {
		checkUCL();
		InputStream is = ucl.getResourceAsStream(name);
		if (is != null)
			return is;
		return super.getResourceAsStream(name);
	}

	/**
   * Checkt, ob die Liste der URLs geaendert wurde und passt den
   * URL-Classloader entsprechend an.
   */
  private void checkUCL()
	{
		// Wir erzeugen das Array nur, wenn wirklich was geaendert wurde.
		// Das hundertfache "toArray()" wuerde sonst ewig dauern.
		if (urlsChanged || urls == null || ucl == null)
		{
			urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
			ucl = new URLClassLoader(urls,getParent());
			urlsChanged = false;
		}
	}

  /**
   * Laedt die angegebene Klasse und initialisiert sie.
   * @param className Name der Klasse.
   * @return Die Klasse.
   * @throws ClassNotFoundException
   */
  public Class load(String className) throws ClassNotFoundException
  {

		// zuerst im Cache schauen.
		Class c = (Class) cache.get(className);
		if (c != null)
			return c;

		checkUCL();
		try {
			// Dann versuchen wir es mit 'nem URLClassLoader, der alle URLs kennt.
			// Wir nehmen deswegen nur einen URLClassloader, damit sichergestellt
			// ist, dass dieser eine alle Plugins und deren Jars kennt.
			// URLClassLoader checken
			return findVia(ucl,className);
		}
		catch (Throwable r) {}

		// Ich weiss, es verstoesst gegen das SUN-Paradigma, dass man zuerst
		// den Parent-Classloader fragen soll. Wir haben es hier aber mit
		// Plugins und deren Jars zu tun. Und die kennt der System-Classloader
		// definitiv nicht.
		try {
			return findVia(getParent(),className);
		}
		catch (Throwable r) {}

		// ok, wir fragen die anderen ClassLoader
    ClassLoader l = null;
    for (int i=0;i<loaders.size();++i)
    {
      try {
        l = (ClassLoader) loaders.get(i);
				return findVia(l,className);
      }
      catch (Throwable t)
      {
      }
    }
    throw new ClassNotFoundException("class not found: " + className);
  }

	/**
	 * Sucht die Klasse ueber den angegebenen ClassLoader.
   * @param loader ClassLoader.
   * @param className Klasse.
   * @return die geladene Klasse.
   * @throws ClassNotFoundException
   */
  private Class findVia(ClassLoader loader, String className) throws ClassNotFoundException
	{
		Class c = loader.loadClass(className);
		// Klasse gefunden. Die tun wir gleich noch in den Cache.
		cache.put(className,c);
		// und registrieren sie im ClassFinder
		finder.addClass(c);
		return c;
	}

  /**
	 * Sucht nach einem Implementor des uebergebenen Interfaces.
	 * Hinweis: Die Funktion liefert generell nur instanziierbare Klassen.
	 * Es werden also weder abstrakte Klassen, noch Interfaces oder RMI-Stubs geliefert.
	 * @param interphase das Interface.
	 * @return gefundene Klasse.
	 * @throws ClassNotFoundException wenn der Implementor nicht gefunden wurde.
	 */
	public Class findImplementor(Class interphase) throws ClassNotFoundException
	{
		return finder.findImplementor(interphase);
	}

	/**
	 * Klassen-Sucher.
	 * Diese Teil hier kann man mit Klassen fuettern und danach in verschiedener Hinsicht befragen.
	 */
	private class ClassFinder
	{

		private Hashtable cache = new Hashtable();
		private ArrayList classes = new ArrayList();

		/**
		 * ct.
		 */
		private ClassFinder()
		{
		}

		/**
		 * Fuegt die Klasse dem Finder hinzu.
		 * @param clazz die Klasse.
		 */
		private void addClass(Class clazz)
		{
			if (isStub(clazz))
				return;

			if (clazz.isInterface() || clazz.isPrimitive())
				return;
			
			classes.add(clazz);
		}

		/**
		 * Sucht nach einem ggf. vorhandenen Implementor des uebergebenen Interfaces.
		 * Hinweis: Die Funktion liefert generell nur instanziierbare Klassen.
		 * Es werden also weder abstrakte Klassen, noch Interfaces oder RMI-Stubs geliefert.
		 * @param interphase das Interface.
		 * @return die gefundene Klasse.
		 * @throws ClassNotFoundException wenn der Implementor nicht gefunden wurde.
		 */
		private Class findImplementor(Class interphase) throws ClassNotFoundException
		{

			if (!interphase.isInterface() &&
					!Modifier.isAbstract(interphase.getModifiers()) &&
					!isStub(interphase)) // kein Interface, nicht abstract, kein Stub
				return interphase; //dann geben wir das Ding so zurueck, wie es ist

			long start = System.currentTimeMillis();
		
			// erstmal im Cache checken
			Class found = (Class) cache.get(interphase);
			if (found != null)
			{
				return found;
			}
			
			Class test = null;

			// So, jetzt geht die Suche los
			// Ggf. muessen wir die Ableitungshierachie hochwandern.
			// Wenn mehrere Klassen das Interface implementieren, sammeln
			// wir diese in einer Ranking-Liste und nehmen am Ende die Klasse,
			// welche das Interface direkt oder am naehesten implementiert.
			ArrayList ranking = new ArrayList();

			// ueber alle Klassen iterieren 
			for (int i=0;i<classes.size();++i)
			{
				test = (Class) classes.get(i);

				// ist ein Stub - koennen wir ganz vergessen
				if (isStub(test))
					continue;

				// checken, ob die Klasse das Interface direkt
				// implementiert und nicht abstrakt ist.
				if (directImplementor(test,interphase) &&
					  !Modifier.isAbstract(test.getModifiers())
				)
				{
					// ok, dann koennen wir die gleich nehmen
					cache.put(interphase,test);
					logger.debug("multipleClassLoader.ClassFinder: found implementor " + test.getName() + " for interface " + interphase.getName());
					logger.debug("multipleClassLoader.ClassFinder:   [used time: " + (System.currentTimeMillis() - start) + " millis]");
					return test;
				}


				// und jetzt die Rekursion
				Class parent = null;
				while (true)
				{
					parent = test.getSuperclass();
					if (parent == null)
						break; // oben angekommen. Naechste Test-Klasse bitte ;)

					if (!parent.isInterface() &&
					    !Modifier.isAbstract(parent.getModifiers()) &&
							directImplementor(parent,interphase))
					{
						// Mhh, ist auf jeden Fall schonmal eine brauchbarer Kandidat.
						ranking.add(parent);
					}
					test = parent; // naechste Iteration
				}
			}


			// OK, direkt implementiert hat's scheinbar niemand.
			// Dann checken wir das Ranking.
			if (ranking.size() == 0)
			{
				// Mift, nix gefunden
				logger.debug("multipleClassLoader.ClassFinder: ...no implementor found for " + interphase.getName());
				throw new ClassNotFoundException("no implementor found for " + interphase.getName());
			}

			// dabei fangen wir hinten an, weil die dem Interface naeher sind
			Class current = null;
			for (int i=ranking.size();i>=0;--i)
			{
				current = (Class) ranking.get(i);
				if (Modifier.isAbstract(current.getModifiers()))
					continue; //Abstrakte Klassen koennen nicht instanziiert werden

				// dann muesste das hier jetzt die erste passende sein
				cache.put(interphase,current);
				logger.debug("multipleClassLoader.ClassFinder: found implementor " + current.getName() + " for interface " + interphase.getName());
				logger.debug("multipleClassLoader.ClassFinder:   [used time: " + (System.currentTimeMillis() - start) + " millis]");
				return current;
			}

			// Mift, nix gefunden
			logger.debug("multipleClassLoader.ClassFinder: ...no implementor found for " + interphase.getName());
			throw new ClassNotFoundException("no implementor found for " + interphase.getName());
		}

		/**
		 * Prueft, ob die Klasse ein Stub oder Skel ist.
     * @param clazz zu pruefende Klasse.
     * @return true wenn es einer ist.
     */
    private boolean isStub(Class clazz)
		{
			return (clazz.getName().endsWith("_Stub") || clazz.getName().endsWith("_Skel"));
		}


		/**
		 * Checkt, ob die Klasse das Interface implementiert.
		 * @param test Test-Klasse.
		 * @param interphase zu pruefendes Interface.
		 * @return true, wenn sie es implementiert.
		 */
		private boolean directImplementor(Class test, Class interphase)
		{
			Class[] interfaces = test.getInterfaces();
			for (int j=0;j<interfaces.length;++j)
			{
				if (interfaces[j].equals(interphase))
				{
					return true;
				}
			}
			return false;
		}
	
	}
}


/*********************************************************************
 * $Log: MultipleClassLoader.java,v $
 * Revision 1.15  2004/04/01 19:02:02  willuhn
 * @N added getResourceAsStream
 *
 * Revision 1.14  2004/04/01 00:23:49  willuhn
 * *** empty log message ***
 *
 * Revision 1.13  2004/03/31 22:50:51  willuhn
 * @B bugfixes in CLassLoader
 * @N massive performance speedup! ;)
 *
 * Revision 1.12  2004/03/30 22:07:19  willuhn
 * *** empty log message ***
 *
 * Revision 1.11  2004/03/29 19:56:56  willuhn
 * @D javadoc
 *
 * Revision 1.10  2004/03/18 01:24:56  willuhn
 * @C refactoring
 *
 * Revision 1.9  2004/03/06 18:24:47  willuhn
 * @D javadoc
 *
 * Revision 1.8  2004/02/27 01:09:42  willuhn
 * *** empty log message ***
 *
 * Revision 1.7  2004/02/26 18:47:11  willuhn
 * *** empty log message ***
 *
 * Revision 1.6  2004/02/25 23:12:07  willuhn
 * *** empty log message ***
 *
 * Revision 1.5  2004/02/09 13:06:51  willuhn
 * @C misc
 *
 * Revision 1.4  2004/01/29 00:45:50  willuhn
 * *** empty log message ***
 *
 * Revision 1.3  2004/01/25 18:40:05  willuhn
 * *** empty log message ***
 *
 * Revision 1.2  2004/01/23 00:24:17  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2004/01/08 21:38:39  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2004/01/05 18:04:46  willuhn
 * @N added MultipleClassLoader
 *
 *********************************************************************/