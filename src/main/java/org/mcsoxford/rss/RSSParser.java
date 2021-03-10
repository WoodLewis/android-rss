/*
 * Copyright (C) 2010 A. Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mcsoxford.rss;

import java.io.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Thread-safe RSS parser SPI implementation.
 * 
 * @author Mr Horn
 */
public class RSSParser implements RSSParserSPI {

  private final RSSConfig config;

  public RSSParser(RSSConfig config) {
    this.config = config;
  }

  /**
   * Parses input stream as RSS feed. It is the responsibility of the caller to
   * close the RSS feed input stream.
   * 
   * @param feed RSS 2.0 feed input stream
   * @return in-memory representation of RSS feed
   * @throws RSSFault if an unrecoverable parse error occurs
   */
  public RSSFeed parse(InputStream feed) {
    try {
      // Since SAXParserFactory implementations are not guaranteed to be
      // thread-safe, a new local object is instantiated.
      final SAXParserFactory factory = SAXParserFactory.newInstance();

      // Support Android 1.6 (see Issue 1)
      factory.setFeature("http://xml.org/sax/features/namespaces", false);
      factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

      final SAXParser parser = factory.newSAXParser();

      return parse(parser, feed);
    } catch (ParserConfigurationException e) {
      throw new RSSFault(e);
    } catch (SAXException e) {
      throw new RSSFault(e);
    } catch (IOException e) {
      throw new RSSFault(e);
    }
  }

  /**
   * Parses input stream as an RSS 2.0 feed.
   * 
   * @return in-memory representation of an RSS feed
   * @throws IllegalArgumentException if either argument is {@code null}
   */
  private RSSFeed parse(SAXParser parser, InputStream feed)
      throws SAXException, IOException {
    if (parser == null) {
      throw new IllegalArgumentException("RSS parser must not be null.");
    } else if (feed == null) {
      throw new IllegalArgumentException("RSS feed must not be null.");
    }

    StringBuffer buffer=new StringBuffer();
    try (BufferedReader reader=new BufferedReader(new InputStreamReader(feed))){
      char[] tmp=new char[1024];
      int size=0;
      while((size=reader.read(tmp))>0){
        buffer.append(tmp,0,size);
      }
    }
    byte[] bytes= buffer.toString().replaceAll(
            "[\ufeff\u0001\u0002\u0003\u0004" +
                    "\u0005\u0006\u0007\u0008" +
                    "\u000b\u000c\u000e\u0010" +
                    "\u0011\u0012\u0013\u0014" +
                    "\u0015\u0016\u0017\u0018" +
                    "\u0019\u001a\u001b\u001c" +
                    "\u001d\u001f\\x00-\\x08\\x0b-\\x0c\\x0e-\\x1f]","").getBytes("UTF-8");
    ByteArrayInputStream stream=new ByteArrayInputStream(bytes);


    // SAX automatically detects the correct character encoding from the stream
    // See also http://www.w3.org/TR/REC-xml/#sec-guessing
    final InputSource source = new InputSource(stream);
    final XMLReader xmlreader = parser.getXMLReader();
    final RSSHandler handler = new RSSHandler(config);

    xmlreader.setContentHandler(handler);
    xmlreader.parse(source);

    return handler.feed();
  }

}

