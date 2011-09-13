// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package adaptorlib;

import static org.junit.Assert.*;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 * Tests for {@link GsaCommunicationHandler}.
 */
public class GsaCommunicationHandlerTest {
  private Config config;
  private GsaCommunicationHandler gsa;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    config = new Config();
    config.setValue("gsa.hostname", "localhost");
    // Let the OS choose the port
    config.setValue("server.port", "0");
    gsa = new GsaCommunicationHandler(new NullAdaptor(), config);
  }

  @After
  public void teardown() {
    gsa.stop(0);
  }

  @Test
  public void testRelativeDot() {
    String docId = ".././hi/.h/";
    URI uri = gsa.encodeDocId(new DocId(docId));
    String uriStr = uri.toString();
    assertFalse(uriStr.contains("/../"));
    assertFalse(uriStr.contains("/./"));
    assertTrue(uriStr.contains("/hi/.h/"));
    assertEquals(docId, gsa.decodeDocId(uri).getUniqueId());
  }

  @Test
  public void testDot() {
    String docId = ".";
    URI uri = gsa.encodeDocId(new DocId(docId));
    String uriStr = uri.toString();
    assertTrue(uriStr.contains("..."));
    assertEquals(docId, gsa.decodeDocId(uri).getUniqueId());
  }

  @Test
  public void testDoubleDot() {
    String docId = "..";
    URI uri = gsa.encodeDocId(new DocId(docId));
    String uriStr = uri.toString();
    assertTrue(uriStr.contains("...."));
    assertEquals(docId, gsa.decodeDocId(uri).getUniqueId());
  }

  @Test
  public void testNotToBeConfusedDots() {
    String docId = "...";
    URI uri = gsa.encodeDocId(new DocId(docId));
    String uriStr = uri.toString();
    assertTrue(uriStr.contains("....."));
    assertEquals(docId, gsa.decodeDocId(uri).getUniqueId());
  }

  @Test
  public void testNotToBeChanged() {
    String docId = "..safe../.h/h./..h/h..";
    URI uri = gsa.encodeDocId(new DocId(docId));
    String uriStr = uri.toString();
    assertTrue(uriStr.contains(docId));
    assertEquals(docId, gsa.decodeDocId(uri).getUniqueId());
  }

  private void decodeAndEncode(String id) {
    URI uri = gsa.encodeDocId(new DocId(id));
    assertEquals(id, gsa.decodeDocId(uri).getUniqueId());
  }

  @Test
  public void testAssortedNonDotIds() {
    decodeAndEncode("simple-id");
    decodeAndEncode("harder-id/");
    decodeAndEncode("harder-id/./");
    decodeAndEncode("harder-id///&?///");
    decodeAndEncode("");
    decodeAndEncode(" ");
    decodeAndEncode(" \n\t  ");
    decodeAndEncode("/");
    decodeAndEncode("//");
    decodeAndEncode("drop/table/now");
    decodeAndEncode("/drop/table/now");
    decodeAndEncode("//drop/table/now");
    decodeAndEncode("//d&op/t+b+e/n*w");
  }

  @Test
  public void testLogRpcMethod() {
    String golden = "Testing\n";

    GsaCommunicationHandler.CircularLogRpcMethod method
        = new GsaCommunicationHandler.CircularLogRpcMethod();
    try {
      Logger logger = Logger.getLogger("");
      Level origLevel = logger.getLevel();
      logger.setLevel(Level.FINEST);
      Logger.getLogger("").finest("Testing");
      logger.setLevel(origLevel);
      String str = (String) method.run(null);
      assertTrue(str.endsWith(golden));
    } finally {
      method.close();
    }
  }

  @Test
  public void testConfigRpcMethod() {
    Map<String, String> golden = new HashMap<String, String>();
    golden.put("gsa.characterEncoding", "UTF-8");
    golden.put("server.hostname", "localhost");

    MockConfig config = new MockConfig();
    config.setKey("gsa.characterEncoding", "UTF-8");
    config.setKey("server.hostname", "localhost");
    GsaCommunicationHandler.ConfigRpcMethod method
        = new GsaCommunicationHandler.ConfigRpcMethod(config);
    Map map = (Map) method.run(null);
    assertEquals(golden, map);
  }

  @Test
  public void testBasicListen() throws Exception {
    gsa.beginListeningForContentRequests();
    URL url = new URL("http", "localhost", config.getServerPort(), "/");
    URLConnection conn = url.openConnection();
    thrown.expect(java.io.FileNotFoundException.class);
    conn.getContent();
  }

  private static class NullAdaptor extends AbstractAdaptor {
    @Override
    public void getDocIds(DocIdPusher pusher) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void getDocContent(Request req, Response resp) {
      throw new UnsupportedOperationException();
    }
  }
}
