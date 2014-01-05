/*
 * Copyright 2013 Bademus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *    Contributors:
 *                 Bademus
 */

package org.github.bademux.feedly.conman;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import org.github.bademux.feedly.api.dev.oauth2.DevFeedlyAuthorizationCodeFlow;
import org.github.bademux.feedly.api.dev.service.DevFeedly;
import org.github.bademux.feedly.api.extensions.java6.auth.oauth2.FeedlyAuthorizationCodeInstalledApp;
import org.github.bademux.feedly.api.extensions.jetty.auth.oauth2.FeedlyLocalServerReceiver;
import org.github.bademux.feedly.api.model.Profile;
import org.github.bademux.feedly.api.oauth2.FeedlyAuthorizationCodeFlow;
import org.github.bademux.feedly.api.oauth2.FeedlyCredential;
import org.github.bademux.feedly.api.service.Feedly;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import static com.google.api.client.repackaged.com.google.common.base.Preconditions.checkState;
import static com.google.api.client.util.Preconditions.checkNotNull;
import static java.lang.System.getProperty;

/** Console manager sample application */
public class FeedlyConMan {

  public static void main(String[] args) {
    initShutdownHook();

    //check if we have commands
    if (args.length > 1) {
      for (int i = 1; i < args.length; i++) {
        try {
          exec(args[i]);
        } catch (Throwable t) {
          t.printStackTrace();
        }
        System.exit(0);
      }
    }

    //init service
    if (service == null) {
      try {
        exec("login");
      } catch (Throwable t) {
        t.printStackTrace();
        System.exit(0);
      }
    }

    Scanner sc = new Scanner(System.in);
    while (!IS_SHUTDOWN) {
      System.out.print("Enter command: ");
      try {
        exec(sc.nextLine());
      } catch (IOException e) {
        System.err.println(e.getMessage());
      } catch (Throwable t) {
        t.printStackTrace();
        IS_SHUTDOWN = true;
      }
    }
  }

  public static void exec(String command) throws Exception {
    switch (command) {
      case "profile":
        Profile profile = checkNotNull(service, "Please authorize").profile().get().execute();
        for (Map.Entry<String, Object> entry : profile.entrySet()) {
          System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        break;
      case "exit":
        IS_SHUTDOWN = true;
        break;
      case "logout":
        checkNotNull(service, "Please authorize").clearCredential();
        deleteDir(DATA_STORE_FACTORY.getDataDirectory());
        System.out.println("Credential is cleared");
        service = null;
        break;
      case "login":
        if (DATA_STORE_FACTORY == null) {
          DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        }
        FeedlyCredential credential = authorize();
        System.out.println("Authorized\n"
                           + " UserId: " + credential.getUserId()
                           + " Plan: " + credential.getPlan());
        //setup Feedly service
        service = new DevFeedly.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).build();
        break;
      case "export":
        String opmlStr = checkNotNull(service, "Please authorize").opml().exportSubscription()
            .executeAndDownloadAsString();
        String opmlFilePath = getJarContainingFolder(FeedlyConMan.class) + '/' + OPML_FILE_NAME;
        System.out.println(opmlFilePath);
        try (PrintWriter writer = new PrintWriter(opmlFilePath, "UTF-8")) {
          writer.write(opmlStr);
        }
        break;
      case "import":
        File opml = new File(getJarContainingFolder(FeedlyConMan.class), OPML_FILE_NAME);
        checkState(opml.exists(), "Please, put '" + opml.getAbsoluteFile()
                                  + "' file to the folder with the program");
        checkNotNull(service, "Please authorize").opml().importSubscription(opml).execute();
        break;
      default:
        System.out.println("Unknown command");
      case "help":
        System.out.println(
            "Available commands:\n"
            + "login   - authenticate the user. User will be redirected to a login web page\n"
            + "logout  - clear credential\n"
            + "profile - shows user information\n"
            + "export  - Downloads feed list to the './" + OPML_FILE_NAME + "' file\n"
            + "import  - uploads opml './" + OPML_FILE_NAME + "' file to the Feedly service\n"
            + "exit    - Exits from the program. User credential still can be stored in '"
            + DATA_STORE_DIR + "' folder. use 'logout' to clear.\n"
            + "help    - shows this menu");
    }
  }

  /** Authorizes the installed application to access user's protected data. */
  private static FeedlyCredential authorize() throws Exception {
    Properties secrets = load("user_secrets.properties");
    String clientId = checkNotNull(secrets.getProperty("feedly.client_id"));
    String clientSecret = checkNotNull(secrets.getProperty("feedly.client_secret"));

    // set up authorization code flow
    FeedlyAuthorizationCodeFlow flow = new DevFeedlyAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret)
        .setDataStoreFactory(DATA_STORE_FACTORY).build();

    // authorize
    FeedlyLocalServerReceiver receiver = new FeedlyLocalServerReceiver.Builder()
        .setPort(8080).build();
    FeedlyAuthorizationCodeInstalledApp a = new FeedlyAuthorizationCodeInstalledApp(flow, receiver);
    return (FeedlyCredential) a.authorize("user");
  }

  public static Properties load(String fileName) throws IOException {
    Properties prop = new Properties();
    prop.load(FeedlyConMan.class.getClassLoader().getResourceAsStream(fileName));
    return prop;
  }

  protected static void initShutdownHook() {
    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        IS_SHUTDOWN = true;
        mainThread.interrupt();
      }
    });
  }

  protected static String getJarContainingFolder(Class aclass) throws Exception {
    CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

    File jarFile;

    if (codeSource.getLocation() != null) {
      jarFile = new File(codeSource.getLocation().toURI());
    } else {
      String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
      String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
      jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
      jarFile = new File(jarFilePath);
    }
    return jarFile.getParentFile().getAbsolutePath();
  }

  protected static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      for (String children : dir.list()) {
        if (!deleteDir(new File(dir, children))) {
          return false;
        }
      }
    }
    return dir.delete(); // The directory is empty now and can be deleted.
  }

  private static Feedly service;

  /** Directory to store user credentials. */
  public static final File DATA_STORE_DIR = new File(getProperty("user.home"), ".store/feedly-api");

  private static final String OPML_FILE_NAME = "feedly.opml";
  /**
   * Global instance of the {@link com.google.api.client.util.store.DataStoreFactory}. The best
   * practice is to make it a single
   * globally shared instance across your application.
   */
  private static FileDataStoreFactory DATA_STORE_FACTORY;

  /** Global instance of the HTTP transport. */
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY = new GsonFactory();

  private static volatile boolean IS_SHUTDOWN = false;
}