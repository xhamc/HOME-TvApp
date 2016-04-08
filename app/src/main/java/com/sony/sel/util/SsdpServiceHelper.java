package com.sony.sel.util;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Xml;

import com.google.gson.Gson;
import com.sony.sel.util.ssdp.SSDP;
import com.sony.sel.util.ssdp.SSDPSearchMsg;
import com.sony.sel.util.ssdp.SSDPSocket;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class SsdpServiceHelper {

  public static final String LOG_TAG = SsdpServiceHelper.class.getSimpleName();

  // service types
  public enum SsdpServiceType {

    ANY(""),
    ROOT_DEVICE("upnp:rootdevice"),
    DIAL("urn:dial-multiscreen-org:service:dial:1"),
    BASIC("urn:schemas-upnp-org:device:Basic:1"),
    AV_TRANSPORT("urn:schemas-upnp-org:service:AVTransport:1"),
    CONNECTION_MANAGER("urn:schemas-upnp-org:service:ConnectionManager:1"),
    CONTENT_DIRECTORY("urn:schemas-upnp-org:service:ContentDirectory:1"),
    SCALAR("urn:schemas-sony-com:service:ScalarWebAPI:1");

    private final String value;

    SsdpServiceType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }


  /**
   * Listener to receive scalar API discovery notification.
   */
  public interface SsdpDeviceObserver {

    /**
     * Callback received when service is discovered.
     *
     * @param deviceInfo Service information that was found.
     */
    void onDeviceFound(@NonNull SsdpDeviceInfo deviceInfo);

    /**
     * Callback received if there's an error during discovery.
     *
     * @param error The error that was encountered.
     */
    void onError(@NonNull Exception error);
  }

  // time to wait for incoming SSDP packets before checking for cancellation and looping.
  private static final int SSDP_RECEIVE_TIMEOUT = 1000;

  private final NetworkHelper networkHelper;
  private DeviceDiscoveryTask discoveryTask;
  private ObserverSet<SsdpDeviceObserver> observers = new ObserverSet<>(SsdpDeviceObserver.class, ObserverSet.DispatchMethod.ON_UI_THREAD);

  public SsdpServiceHelper(NetworkHelper networkHelper) {
    this.networkHelper = networkHelper;
  }

  /**
   * Find SSDP devices that support a given service type.
   * <p>
   * The observer is always notified on the UI thread.
   *
   * @param observer Observer to receive the scalar API info, or error notification.
   */
  public void findSsdpDevice(@NonNull SsdpServiceType serviceType, @NonNull SsdpDeviceObserver observer) {
    if (observer == null) {
      throw new IllegalArgumentException("Observer can't be null.");
    }
    observers.add(observer);
    if (discoveryTask == null) {
      // need to kick off discovery
      discoveryTask = new DeviceDiscoveryTask(SSDP_RECEIVE_TIMEOUT, 0);
      discoveryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serviceType);
    }
  }

  /**
   * Cancel discovery and clear list of observers.
   *
   * @return true if discovery was in progress and cancel is initiated.
   */
  public boolean cancelDiscovery() {
    if (discoveryTask != null) {
      discoveryTask.cancel(true);
      discoveryTask = null;
      observers.clear();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Access the observer set to add/remove observers.
   */
  public ObserverSet<SsdpDeviceObserver> getObservers() {
    return observers;
  }

  /**
   * Task for performing SSDP service discovery.
   */
  private class DeviceDiscoveryTask extends AsyncTask<SsdpServiceType, SsdpDeviceInfo, Void> {

    private final int dataTimeout;
    private final long stopTime;
    private Exception error;
    private InetAddress ipAddress;

    /**
     * Create a discovery task.
     *
     * @param dataTimeout Timeout (ms) to wait for incoming data before looping.
     * @param timeout     Timeout (ms) to quit the task.
     */
    private DeviceDiscoveryTask(int dataTimeout, long timeout) {

      this.dataTimeout = dataTimeout;
      if (timeout > 0) {
        this.stopTime = System.currentTimeMillis() + timeout;
      } else {
        this.stopTime = 0;
      }
    }

    @Override
    protected Void doInBackground(SsdpServiceType... serviceTypes) {
      SSDPSocket socket = null;
      try {
        Log.d(LOG_TAG, "Starting Scalar discovery.");
        // find current address
        List<InetAddress> addresses = networkHelper.getLocalIpAddresses();
        if (addresses.size() == 0) {
          throw new IOException("Device has no IP address");
        }
        // use first IPv4 address found
        for (InetAddress address : addresses) {
          if (address instanceof Inet4Address) {
            ipAddress = address;
            Log.d(LOG_TAG, "Local IP address = " + ipAddress.getHostAddress() + ".");
            break;
          }
        }
        // set up a socket
        socket = new SSDPSocket(ipAddress);
        // set max timeout to wait for incoming packets
        socket.setTimeout(dataTimeout);
        // search message to discover all available services
        SSDPSearchMsg searchMsg = new SSDPSearchMsg("ssdp:all");
        socket.send(searchMsg.toString());
        while (!isCancelled()) {
          try {
            // check a response packet that was received
            SsdpDeviceInfo ssdpDeviceInfo = parseDatagram(socket.responseReceive(), serviceTypes);
            if (ssdpDeviceInfo != null) {
              // scalar info was discovered for this datagram
              Log.d(LOG_TAG, "Found SSDP service: " + ssdpDeviceInfo);
              publishProgress(ssdpDeviceInfo);
            }
          } catch (InterruptedIOException e) {
            Log.d(LOG_TAG, "Timed out waiting for SSDP response.");
            // responseReceive() timed out waiting for the next packet
            // just continue looping
          }
          if (stopTime > 0 && System.currentTimeMillis() >= stopTime) {
            throw new TimeoutException("Service discovery reached timeout.");
          }
        }
      } catch (UnknownHostException e) {
        // problem resolving host name
        Log.e(LOG_TAG, "Error resolving host: " + e);
        error = e;
      } catch (IOException e) {
        // problem getting/parsing packets
        Log.e(LOG_TAG, "I/O Error: " + e);
        error = e;
      } catch (Exception e) {
        // other problem
        Log.e(LOG_TAG, "An error occurred: " + e);
        error = e;
      } finally {
        if (socket != null) {
          socket.close();
        }
      }
      // error occurred or timed out
      return null;
    }

    @Override
    protected void onProgressUpdate(SsdpDeviceInfo... values) {
      super.onProgressUpdate(values);
      for (SsdpDeviceInfo info : values) {
        observers.proxy().onDeviceFound(info);
      }
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      if (error != null) {
        // notify errors
        observers.proxy().onError(error);
      }
      observers.clear();
      discoveryTask = null;
    }

    /**
     * Parse a UDP datagram packet to figure out the friendly name of the local device.
     *
     * @param packet The UDP packet to parse.
     * @return The friendly name of the discovered device, if found.
     */
    private SsdpDeviceInfo parseDatagram(DatagramPacket packet, SsdpServiceType[] serviceTypes) {
      // convert to a more readable packet format
      SSDP.ParsedDatagram datagram = SSDP.convertDatagram(packet);
      // for UPnP devices, LOCATION will contain the URL of the description.xml file
      String location = datagram.getData().get(SSDP.LOCATION);
      // otherwise ST will contain the URN of the service type
      String st = datagram.getData().get(SSDP.ST);
      if (location != null) {
        Log.d(LOG_TAG, "SSDP service discovered. Type = " + (st != null ? st : "null") + ", Location = " + location + ".");
        try {
          // check if the service is the specified type
          if (serviceTypes != null) {
            for (SsdpServiceType serviceType : serviceTypes) {
              if (serviceType == SsdpServiceType.ANY || serviceType.getValue().equals(st)) {
                // read description xml from location and extract the model name
                Log.d(LOG_TAG, "Service type matches " + serviceType + ".");
                return SsdpDeviceInfo.fromLocation(location);
              }
            }
          } else {
            Log.d(LOG_TAG, "No service type filter defined. Returning service info.");
            return SsdpDeviceInfo.fromLocation(location);
          }
        } catch (MalformedURLException e) {
          Log.e(LOG_TAG, "Datagram parsing error: " + e);
          error = e;
          return null;
        } catch (SAXException e) {
          Log.e(LOG_TAG, "Datagram parsing error: " + e);
          error = e;
          return null;
        } catch (IOException e) {
          Log.e(LOG_TAG, "Datagram parsing error: " + e);
          error = e;
          return null;
        }
      }
      // not discovered
      return null;
    }

  }

  public static class IconInfo {

    private int width;
    private int height;
    private int depth;
    private String mimetype;
    private String url;

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    public int getHeight() {
      return height;
    }

    public void setHeight(int height) {
      this.height = height;
    }

    public int getDepth() {
      return depth;
    }

    public void setDepth(int depth) {
      this.depth = depth;
    }

    public String getMimetype() {
      return mimetype;
    }

    public void setMimetype(String mimetype) {
      this.mimetype = mimetype;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }

  public static class SsdpDeviceInfo {

    private String deviceType;
    private String friendlyName;
    private String manufacturer;
    private String modelName;
    private String udn;

    private String deviceDescriptorLocation;
    private List<String> serviceTypes = new ArrayList<>();
    private List<IconInfo> icons = new ArrayList<>();

    private SsdpDeviceInfo() {

    }

    @Override
    public String toString() {
      return new Gson().toJson(this);
    }

    public String getFriendlyName() {
      return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
      this.friendlyName = friendlyName;
    }

    public String getDeviceDescriptorLocation() {
      return deviceDescriptorLocation;
    }

    public void setDeviceDescriptorLocation(String deviceDescriptorLocation) {
      this.deviceDescriptorLocation = deviceDescriptorLocation;
    }

    public List<String> getServiceTypes() {
      return serviceTypes;
    }

    public String getDeviceType() {
      return deviceType;
    }

    public void setDeviceType(String deviceType) {
      this.deviceType = deviceType;
    }

    public String getManufacturer() {
      return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
      this.manufacturer = manufacturer;
    }

    public String getModelName() {
      return modelName;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public String getUdn() {
      return udn;
    }

    public void setUdn(String udn) {
      this.udn = udn;
    }

    public List<IconInfo> getIcons() {
      return icons;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof SsdpDeviceInfo) {
        // compare UDN for equality
        return udn.equals(((SsdpDeviceInfo) o).udn);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return udn.hashCode();
    }

    public static SsdpDeviceInfo fromLocation(String location) throws IOException, SAXException {
      InputStream is = null;
      try {
        URL url = new URL(location);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        is = new BufferedInputStream(connection.getInputStream());
        SsdpDeviceInfo ssdpDeviceInfo = new SsdpDeviceInfo();
        ssdpDeviceInfo.setDeviceDescriptorLocation(location);
        ScalarDeviceDescriptorParser extractor = new ScalarDeviceDescriptorParser(ssdpDeviceInfo);
        Xml.parse(is, Xml.Encoding.UTF_8, extractor);
        return ssdpDeviceInfo;
      } finally {
        if (is != null) {
          try {
            // clean up
            is.close();
          } catch (IOException e) {
            // ignore close exceptions
          }
        }
      }
    }

    static class ScalarDeviceDescriptorParser implements ContentHandler {

      private final SsdpDeviceInfo info;
      private StringBuilder currentString;

      private static final String FRIENDLY_NAME = "friendlyName";
      private static final String DEVICE_TYPE = "deviceType";
      private static final String MANUFACTURER = "manufacturer";
      private static final String MODEL_NAME = "modelName";
      private static final String UDN = "UDN";
      private static final String SERVICE_TYPE = "serviceType";

      private static final String ICON = "icon";
      private static final String WIDTH = "width";
      private static final String HEIGHT = "height";
      private static final String URL = "url";
      private static final String MIME_TYPE = "mimetype";
      private static final String DEPTH = "depth";

      private IconInfo icon;

      public ScalarDeviceDescriptorParser(SsdpDeviceInfo info) {
        this.info = info;
      }

      @Override
      public void setDocumentLocator(Locator locator) {

      }

      @Override
      public void startDocument() throws SAXException {

      }

      @Override
      public void endDocument() throws SAXException {

      }

      @Override
      public void startPrefixMapping(String prefix, String uri) throws SAXException {

      }

      @Override
      public void endPrefixMapping(String prefix) throws SAXException {

      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        currentString = new StringBuilder();
        if (ICON.equals(localName)) {
          icon = new IconInfo();
        }
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {

        if (DEVICE_TYPE.equals(localName)) {
          info.setDeviceType(currentString.toString().trim());
        } else if (FRIENDLY_NAME.equals(localName)) {
          info.setFriendlyName(currentString.toString().trim());
        } else if (MANUFACTURER.equals(localName)) {
          info.setManufacturer(currentString.toString().trim());
        } else if (MODEL_NAME.equals(localName)) {
          info.setModelName(currentString.toString().trim());
        } else if (UDN.equals(localName)) {
          info.setUdn(currentString.toString().trim());
        } else if (SERVICE_TYPE.equals(localName)) {
          info.getServiceTypes().add(currentString.toString().trim());
        } else if (ICON.equals(localName)) {
          // save icon
          info.getIcons().add(icon);
          icon = null;
        } else if (icon != null) {
          if (WIDTH.equals(localName)) {
            icon.setWidth(Integer.valueOf(currentString.toString().trim()));
          } else if (HEIGHT.equals(localName)) {
            icon.setHeight(Integer.valueOf(currentString.toString().trim()));
          } else if (MIME_TYPE.equals(localName)) {
            icon.setMimetype(currentString.toString().trim());
          } else if (URL.equals(localName)) {
            icon.setUrl(currentString.toString().trim());
          } else if (DEPTH.equals(localName)) {
            icon.setDepth(Integer.valueOf(currentString.toString().trim()));
          }
        }
        currentString = null;
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentString != null) {
          currentString.append(ch, start, length);
        }
      }

      @Override
      public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

      }

      @Override
      public void processingInstruction(String target, String data) throws SAXException {

      }

      @Override
      public void skippedEntity(String name) throws SAXException {

      }
    }
  }
}