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

package de.willuhn.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.willuhn.logging.Logger;

/**
 * Util-Klasse mit Hilfsfunktionen fuer Streams.
 */
public class IOUtil
{
  /**
   * Kopiert die Daten vom InputStream in den OutputStream.
   * @param is Quell-Stream.
   * @param os Ziel-Stream.
   * @return Anzahl der kopierten Bytes.
   * @throws IOException
   */
  public static long copy(InputStream is, OutputStream os) throws IOException
  {
    byte b[] = new byte[4096];
    long r = 0;
    int read = 0;
    while ((read = is.read(b)) != -1)
    {
      if (read > 0) // Nur schreiben, wenn wirklich was gelesen wurde
      {
        os.write(b,0,read);
        r += read;
      }
    }
    return r;
  }
  
  /**
   * Schliesst Streams.
   * Ggf. auftretende Exceptions werden nicht weitergeworfen sondern nur geloggt.
   * @param closeables Liste zu schliessender Streams. NULL wird toleriert.
   * @return true, wenn das Schliessen erfolgreich war, sonst false.
   */
  public static boolean close(Closeable... closeables)
  {
    boolean ok = true;
    
    if (closeables == null || closeables.length == 0)
      return ok;

    for (Closeable c:closeables)
    {
      try
      {
        if (c != null)
          c.close();
      }
      catch (Throwable t)
      {
        Logger.error("error while closing stream",t);
        ok = false;
      }
    }
    return ok;
  }
}



/**********************************************************************
 * $Log: IOUtil.java,v $
 * Revision 1.3  2010/12/07 16:09:43  willuhn
 * @N NULL tolerieren
 *
 * Revision 1.2  2010-12-07 16:07:10  willuhn
 * @N Mehrere Streams schliessen
 *
 * Revision 1.1  2010-12-07 16:01:53  willuhn
 * @N IOUtil
 *
 **********************************************************************/