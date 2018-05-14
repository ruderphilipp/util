/**********************************************************************
 *
 * Copyright (c) 2004 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * GNU LESSER GENERAL PUBLIC LICENSE 2.1.
 * Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Liefert Informationen ueber die Plattform.
 */
public class Platform
{
  /**
   * Enumerations fuer das OS.
   */
  public static enum OS
  {
    /**
     * Windows 32Bit.
     */
    WIN32,
    
    /**
     * Windows 64Bit.
     */
    WIN64,
    
    /**
     * Linux 32Bit.
     */
    LINUX32,
    
    /**
     * Linux 64Bit.
     */
    LINUX64,
    
    /**
     * Apple OS X 32Bit.
     */
    MACOS32,
    
    /**
     * Apple OS X 64Bit.
     */
    MACOS64,
    
    /**
     * FreeBSD 32Bit.
     */
    FREEBSD32,
    
    /**
     * FreeBSD 64Bit.
     */
    FREEBSD64,
    
    /**
     * Plattform nicht unterstuetzt.
     */
    UNKNOWN
  }

  /**
   * Liefert das Verzeichnis, in dem sich die uebergebene Klasse befindet.
   * @param c die Klasse, zu der das Verzeichnis gesucht wird.
   * @return Verzeichnis, in dem sich die Klasse befindet.
   * @throws IOException
   */
  public static File getDir(Class c) throws IOException
  {
    try
    {
      URL url = c.getProtectionDomain().getCodeSource().getLocation();
      File f = new File(url.toURI());
      if (f.isFile())
      {
        // ist ne Jar-Datei - dann liefern wir das Verzeichnis, in dem sie sich befindet
        return f.getParentFile();
      }
      return f;
    }
    catch (URISyntaxException e)
    {
      throw new IOException("unable to determine dir for class " + c + ": " + e.getMessage());
    }
  }
  
  /**
   * Liefert die Kennung der Plattform.
   * @return Kennung der Plattform.
   */
  public static OS getPlatform()
  {
    String os = System.getProperty("os.name");
    String arch = System.getProperty("os.arch");

    if (os.toLowerCase().indexOf("linux") != -1)
      return (arch.toLowerCase().indexOf("64") != -1) ? OS.LINUX64 : OS.LINUX32;
    
    if (os.toLowerCase().indexOf("windows") != -1)
      return (arch.toLowerCase().indexOf("64") != -1) ? OS.WIN64 : OS.WIN32;

    if (os.toLowerCase().indexOf("mac") != -1)
      return (arch.toLowerCase().indexOf("64") != -1) ? OS.MACOS64 : OS.MACOS32;

    if (os.toLowerCase().indexOf("freebsd") != -1)
      return (arch.toLowerCase().indexOf("64") != -1) ? OS.FREEBSD64 : OS.FREEBSD32;

    return OS.UNKNOWN;
  }
}



/**********************************************************************
 * $Log: Platform.java,v $
 * Revision 1.3  2012/02/11 13:50:56  willuhn
 * *** empty log message ***
 *
 * Revision 1.2  2010-09-29 10:47:39  willuhn
 * @B den Konstruktor gibts erst in Java 1.6
 *
 * Revision 1.1  2010-09-29 10:44:35  willuhn
 * @N Ein Jar-Loader und ein Platform-Util
 *
 * Revision 1.1  2010/09/28 16:40:38  willuhn
 * @N initial checkin
 *
 **********************************************************************/