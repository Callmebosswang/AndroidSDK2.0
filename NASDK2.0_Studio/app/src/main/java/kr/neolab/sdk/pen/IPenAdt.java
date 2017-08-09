package kr.neolab.sdk.pen;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.util.UseNoteData;

/**
 * Pen and an adapter class that defines the communication interface
 * <p>
 * Bluetooth communication is currently supported.
 * (If you want a different way to implement this interface must be provided.)
 *
 * @author CHY
 */
public interface IPenAdt
{
    // 아래 값들은 BTAdt와 BTLEAdt에서 공통으로 사용하며, IPenAdt에서 접근할 수 있어야하는 부분이 있으므로 IPenAdt로 위치를 변경함
    /**
     * The constant CONN_STATUS_IDLE.
     */
    int CONN_STATUS_IDLE = 0x01;
    /**
     * The constant CONN_STATUS_BINDED.
     */
    int CONN_STATUS_BINDED = 0x02;
    /**
     * The constant CONN_STATUS_ESTABLISHED.
     */
    int CONN_STATUS_ESTABLISHED = 0x03;
    /**
     * The constant CONN_STATUS_AUTHORIZED.
     */
    int CONN_STATUS_AUTHORIZED = 0x04;
    /**
     * The constant CONN_STATUS_TRY.
     */
    int CONN_STATUS_TRY = 0x05;
    /**
     * set up listener of message from pen
     *
     * @param listener callback interface
     */
    public void setListener(IPenMsgListener listener);

    /**
     * set up listener of dot message from pen
     *
     * @param listener the listener
     */
    public void setDotListener(IPenDotListener listener);

    /**
     * set up listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @param listener callback interface
     */
    public void setOffLineDataListener(IOfflineDataListener listener);

    /**
     * get up listener of message from pen
     *
     * @return IPenMsgListener listener
     */
    public IPenMsgListener getListener();

    /**
     * get up listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @return IOfflineDataListener off line data listener
     */
    public IOfflineDataListener getOffLineDataListener();

    /**
     * Attempts to connect to the pen.
     *
     * @param address MAC address of pen
     */
    public void connect(String address);

    /**
     * And disconnect the connection with pen
     */
    public void disconnect();

    /**
     * Wait for connections from a pen.
     */
    public void startListen();

    /**
     * Confirm whether or not the MAC address to connect
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     */
    public boolean isAvailableDevice( String mac ) throws BLENotSupportedException;

    /**
     * Confirm whether or not the MAC address to connect
     * NOTICE
     * SPP must use mac address bytes
     * BLE must use advertising full data ( ScanResult.getScanRecord().getBytes() )
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     */
    public boolean isAvailableDevice( byte[] mac );

    /**
     * Connected to the pen's current information.
     *
     * @return connected device
     */
    public String getConnectedDevice();

    /**
     * Gets connecting device.
     *
     * @return the connecting device
     */
    public String getConnectingDevice();

    /**
     * When pen requested password, you can response password by this method.
     *
     * @param password the password
     */
    public void inputPassword( String password );

    /**
     * Change the password of pen.
     *
     * @param oldPassword current password
     * @param newPassword new password
     */
    public void reqSetupPassword( String oldPassword, String newPassword );

    /**
     * Req set up password off.
     * supported from Protocol 2.0
     *
     * @param oldPassword the old password
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetUpPasswordOff( String oldPassword ) throws ProtocolNotSupportedException;

    /**
     * Connected to the current state of the pen provided.
     */
    public void reqPenStatus();

    /**
     * To upgrade the firmware of the pen.
     *
     * @param fwFile     object of firmware
     * @param targetPath The file path to be stored in the pen
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated reqFwUpgrade(File fwFile, String targetPath) is replaced by reqFwUpgrade2( File fwFile, String fwVersion ) in the protocol 2.0
     */
    public void reqFwUpgrade(File fwFile, String targetPath) throws ProtocolNotSupportedException;

    /**
     * Req fw upgrade 2.
     * supported from Protocol 2.0
     * isCompress default true
     *
     * @param fwFile    the fw file
     * @param fwVersion the fw version
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqFwUpgrade2( File fwFile, String fwVersion) throws ProtocolNotSupportedException;

    /**
     * Req fw upgrade 2.
     *
     * @param fwFile     the fw file
     * @param fwVersion  the fw version
     * @param isCompress data compress true, uncompress false
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqFwUpgrade2( File fwFile, String fwVersion ,boolean isCompress)  throws ProtocolNotSupportedException;

    /**
     * To suspend Upgrading task.
     */
    public void reqSuspendFwUpgrade();

    /**
     * Adjust the pressure-sensor to the pen.
     */
    public void reqForceCalibrate();

    /**
     * Specify whether you want to get the data off-line.
     *
     * @param allow if allow receive offline data, set true
     */
    public void setAllowOfflineData(boolean allow);

    /**
     * Specify where to store the offline data. (Unless otherwise specified, is stored in the default external storage)
     *
     * @param path Be stored in the directory
     */
    public void setOfflineDataLocation(String path);

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteIds   array of note id
     */
    public void reqAddUsingNote(int sectionId, int ownerId, int[] noteIds);

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     */
    public void reqAddUsingNote(int sectionId, int ownerId);

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section ids of note
     * @param ownerId   owner ids of note
     */
    public void reqAddUsingNote(int[] sectionId, int[] ownerId);

    /**
     * Specifies that all of the available notes.
     * Note! It overwrites the using note data from protocol 2.0 .
     */
    public void reqAddUsingNoteAll();

    /**
     * Notes for use in applications specified.
     * supported from Protocol 2.0
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param noteList the note list
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqAddUsingNote(ArrayList<UseNoteData> noteList) throws ProtocolNotSupportedException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     */
    public void reqOfflineData(int sectionId, int ownerId, int noteId);

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException;

    /**
     * The offline data is stored in the pen to request information.
     */
    public void reqOfflineDataList();

    /**
     * The offline data is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineDataList(int sectionId, int ownerId) throws ProtocolNotSupportedException;

    /**
     * The offline data per page is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineDataPageList(int sectionId, int ownerId, int noteId) throws ProtocolNotSupportedException;

    /**
     * To Delete offline data of pen
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated removeOfflineData(int sectionId, int ownerId) is replaced by removeOfflineData( int sectionId, int ownerId ,int[] noteIds) in the protocol 2.0
     */
    public void removeOfflineData(int sectionId, int ownerId) throws ProtocolNotSupportedException;

    /**
     * Remove offline data.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteIds   the note ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void removeOfflineData( int sectionId, int ownerId ,int[] noteIds) throws ProtocolNotSupportedException;

    /**
     * Disable or enable Auto Power function
     *
     * @param setOn the set on
     */
    public void reqSetupAutoPowerOnOff( boolean setOn );

    /**
     * Disable or enable sound of pen
     *
     * @param setOn the set on
     */
    public void reqSetupPenBeepOnOff( boolean setOn );

    /**
     * Setup color of pen
     *
     * @param color the color
     */
    public void reqSetupPenTipColor( int color );

    /**
     * Setup auto shutdown time of pen
     *
     * @param minute shutdown wait time of pen
     */
    public void reqSetupAutoShutdownTime( short minute );

    /**
     * Setup Sensitivity level of pen
     *
     * @param level sensitivity level (0~4)
     */
    public void reqSetupPenSensitivity( short level );

    /**
     * Req set pen cap on off.
     * supported from Protocol 2.0
     *
     * @param on the on
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenCapOff ( boolean on ) throws ProtocolNotSupportedException;

    /**
     * Req setup pen hover.
     *
     * @param on the on
     */
    public void reqSetupPenHover ( boolean on );

    /**
     * set Context
     *
     * @param context the context
     */
    public void setContext( Context context );

    /**
     * get Context
     *
     * @return Context context
     */
    public Context getContext();

    /**
     * Gets protocol version.
     * supported from Protocol 2.0
     *
     * @return the protocol version
     */
    public int getProtocolVersion();

    /**
     * Gets pen status.
     *
     * @return the pen status
     */
    public int getPenStatus ();

    /**
     * Get connected status
     *
     * @return true is connected, false is disconnected
     */
    public boolean isConnected();

    /**
     * Get Pen Mac Address
     * BT Mac address is Real mac address
     * BLE mac address is virtual mac address
     *
     * @return the pen address
     */
    public String getPenAddress();

    /**
     * Get Pen Bt Name
     *
     * @return the bt name
     */
    public String getPenBtName();

}
