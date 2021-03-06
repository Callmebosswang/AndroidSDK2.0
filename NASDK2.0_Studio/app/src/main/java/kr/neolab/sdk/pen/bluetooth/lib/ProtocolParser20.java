package kr.neolab.sdk.pen.bluetooth.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import kr.neolab.sdk.pen.bluetooth.cmd.FwUpgradeCommand20;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.UseNoteData;

/**
 * The type Protocol parser 20.
 *
 * @author Moo
 */
public class ProtocolParser20
{
    /**
     * The constant PKT_RESULT_SUCCESS.
     */
    public static final int PKT_RESULT_SUCCESS = 0x00;
    /**
     * The constant PKT_RESULT_FAIL.
     */
    public static final int PKT_RESULT_FAIL = 0x01;
    /**
     * The constant PKT_RESULT_FAIL2.
     */
    public static final int PKT_RESULT_FAIL2 = 0x02;


    private static final int PKT_ESCAPE = 0x20;

    private static final int PKT_START = 0xC0;
    private static final int PKT_END = 0xC1;
    private static final int PKT_EMPTY = 0x00;
    /**
     * The constant PKT_DLE.
     */
    public static final int PKT_DLE = 0x7D;

    private static final int PKT_CMD_POS = 0;
    private static final int PKT_ERROR_POS = 1;
    private static final int PKT_LENGTH_POS1 = 2;
    private static final int PKT_LENGTH_POS2 = 3;
    private static final int PKT_MAX_LEN = 32 * 1024;

    private int counter = 0;
    private int dataLength = 0;
    private int headerLength = 0;

    // length
    private byte[] lbuffer = new byte[2];

    private static int buffer_size = PKT_MAX_LEN + 1;

    private ByteBuffer nbuffer = ByteBuffer.allocate( buffer_size );
    private ByteBuffer escapeBuffer = ByteBuffer.allocate( buffer_size );

//    private boolean isStart = true;
    private boolean isEvent = false;
    private boolean isDle = false;

    private IParsedPacketListener listener = null;

    /**
     * Instantiates a new Protocol parser 20.
     *
     * @param listener the listener
     */
    public ProtocolParser20 ( IParsedPacketListener listener )
    {
        this.listener = listener;
    }

    /**
     * Parse byte data.
     *
     * @param data the data
     * @param size the size
     */
    public void parseByteData ( byte data[], int size )
    {
        NLog.d( "[ProtocolParser20] parseByteData " + "Packet:" + PacketBuilder.showPacket( data, size ) );
        // StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < size; i++ )
        {
            // int int_data = (int) (data[i] & 0xFF);
            //
            // sb.append(Integer.toHexString(int_data));
            // sb.append(", ");
            //
            // if ( int_data == 0xC1 )
            // {
            // NLog.d("[CommProcessor] parseByteData : " + sb.toString());
            // sb = new StringBuffer();
            // }

            parseOneByteDataEscape( data[i]);
        }

        // NLog.d("[CommProcessor] parseByteData : " + sb.toString());
    }

    private void parseOneByteDataEscape ( byte data)
    {
        int int_data = (int) ( data & 0xFF );
        if ( int_data == PKT_START )
        {
//            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_START" );
            counter = 0;
            nbuffer.clear();
            return;
        }
        else if ( int_data == PKT_END )
        {
//            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_END" );
//            nbuffer.put( counter, data );
            byte[] temp = nbuffer.array();
            int size = counter;
            NLog.d( "parseOneByteDataEscape=size=" + size + "Packet:" + PacketBuilder.showPacket( temp , size));

            for(int i = 0; i < size; i++)
            {
                parseOneByte( temp[i],i, size);
//                    break;
            }
            temp = null;
            counter = 0;
            nbuffer.clear();
            return;
        }
        else if(int_data == PKT_DLE && !isDle )
        {
//            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_DLE" );
            isDle = true;
            return;
        }
        else
        {
            if(isDle)
            {
                data = escapeData(data);
                int t_data = (int) ( data & 0xFF );
            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_DLE = "+t_data );
                isDle = false;
            }
        }
        nbuffer.put( counter, data );
        counter++;
    }

    private byte escapeData ( byte source )
    {
        return (byte) ( source ^ PKT_ESCAPE );
    }


    private boolean parseOneByte ( byte data, int count, int end)
    {
        int int_data = (int) ( data & 0xFF );
//        NLog.d( "[ProtocolParser20] parseOneByte int_data="+int_data+",count="+count +"end="+end);


        if (count == 0)
        {
            NLog.d( "[ProtocolParser20] parseOneByte CMD" );
            counter = 0;
            headerLength = 0;
            escapeBuffer.clear();
        }
        if ( count == end  -1 )
        {
            escapeBuffer.put( counter, data );
            counter++;
            NLog.d( "[ProtocolParser20] parseOneByte PKT_END count=" + count + ", dataLength=" + dataLength + ", headerLength=" + headerLength +", isEvent="+isEvent+ "Packet:" + PacketBuilder.showPacket( escapeBuffer.array(), counter ) );
            if(count == dataLength + headerLength - 1)
            {
                this.listener.onCreatePacket( new Packet( escapeBuffer.array(),2 ,isEvent) );

                dataLength = 0;
                counter = 0;
                escapeBuffer.clear();
            }
            return true;
        }
        else
        {
            if ( counter == PKT_CMD_POS )
            {
                if (CMD20.isEventCMD( data ))
                {
                    isEvent = true;
                }
                else
                    isEvent = false;
                headerLength++;

            }
            else
            {
                if ( isEvent )
                {
                    if ( counter == PKT_LENGTH_POS1 -1 )
                    {
                        lbuffer[0] = data;
                        headerLength++;

                    }
                    else if ( counter == PKT_LENGTH_POS2 - 1 )
                    {
                        lbuffer[1] = data;
                        dataLength = ByteConverter.byteArrayToShort( lbuffer );
                        headerLength++;

                    }

                }
                else
                {
                    if(counter == PKT_ERROR_POS )
                    {
                        headerLength++;
                        if(data != PKT_RESULT_SUCCESS)
                        {
                            escapeBuffer.put( counter, data );
                            this.listener.onCreatePacket( new Packet( escapeBuffer.array(), 2 ) );

                            dataLength = 0;
                            counter = 0;
                            headerLength = 0;
                            escapeBuffer.clear();
                            return true;
                        }
                    }
                    else if ( counter == PKT_LENGTH_POS1 )
                    {
                        lbuffer[0] = data;
                        headerLength++;
                    }
                    else if ( counter == PKT_LENGTH_POS2 )
                    {
                        lbuffer[1] = data;
                        dataLength = ByteConverter.byteArrayToShort( lbuffer );
                        headerLength++;
                    }
                }
            }

            escapeBuffer.put( counter, data );

            counter++;
        }
        return false;
    }

//    // 0x02 CMD20.P_PenOnResponse
//    public static byte[] buildPenOnOffData ( boolean status )
//    {
//        PacketBuilder builder = new PacketBuilder( 9 );
//
//        builder.setCommand( CMD20.P_PenOnResponse );
//
//        byte[] buffer = ByteConverter.longTobyte( System.currentTimeMillis() );
//
//        builder.write( buffer, 8 );
//
//        if ( status )
//        {
//            builder.write( (byte) 0x00 );
//        }
//        else
//        {
//            builder.write( (byte) 0x01 );
//        }
//
//        return builder.getPacket();
//    }

    /**
     * Build req pen info byte [ ].
     *
     * @param appVer    the app ver
     * @param masterKey the master key
     * @return the byte [ ]
     */
// 0x01 CMD20.REQ_PenInfo
    public static byte[] buildReqPenInfo ( String appVer )
    {

        PacketBuilder builder = new PacketBuilder( 16 + 2 + 16);

        builder.setCommand( CMD20.REQ_PenInfo );

        builder.write( ByteConverter.stringTobyte( "" ), 16 );
        // type Android
        builder.write( ByteConverter.shortTobyte( (short) 0x1101 ), 2 );
        // App version 상황에 따라 빈값 일수도 있다.(Context 세팅 유무에 따라)
        builder.write( ByteConverter.stringTobyte( appVer ), 16 );
        NLog.d( "[ProtocolParser20] REQ  buildReqPenInfo. appVer=" + appVer + "Packet:" + builder.showPacket());
        return builder.getPacket();
    }

    /**
     * Build password input byte [ ].
     *
     * @param password the password
     * @return the byte [ ]
     */
// 0x02 CMD20.REQ_Password
    public static byte[] buildPasswordInput ( String password )
    {

        PacketBuilder sendbyte = new PacketBuilder( 16 );
        sendbyte.setCommand( CMD20.REQ_Password );
        sendbyte.write( ByteConverter.stringTobyte( password ), 16 );
        NLog.d( "[ProtocolParser20] REQ  buildPasswordInput." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build password setup byte [ ].
     *
     * @param isUse       the is use
     * @param oldPassword the old password
     * @param newPassword the new password
     * @return the byte [ ]
     */
// 0x03 CMD20.REQ_PasswordSet
    public static byte[] buildPasswordSetup(boolean isUse, String oldPassword, String newPassword )
    {
        PacketBuilder sendbyte = new PacketBuilder( 32 +1);
        sendbyte.setCommand( CMD20.REQ_PasswordSet );
        sendbyte.write( (byte) ( isUse ? 1 : 0 ) );
        sendbyte.write( ByteConverter.stringTobyte( oldPassword ), 16 );
        sendbyte.write( ByteConverter.stringTobyte( newPassword ), 16 );

        NLog.d( "[ProtocolParser20] REQ buildPasswordSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen status data byte [ ].
     *
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus
    public static byte[] buildPenStatusData ()
    {
        PacketBuilder builder = new PacketBuilder( 0 );
        builder.setCommand( CMD20.REQ_PenStatus );

        NLog.d( "[ProtocolParser20] REQ buildPenStatusData." + "Packet:" + builder.showPacket() );
        return builder.getPacket();
    }

    /**
     * Build set current time data byte [ ].
     *
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x01 REQ_PenStatusChange_TYPE_CurrentTimeSet
    public static byte[] buildSetCurrentTimeData ()
    {
        PacketBuilder sendbyte = new PacketBuilder( 9 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_CurrentTimeSet ), 1 );

//        long ts = TimeUtil.convertLocalTimeToUTC( System.currentTimeMillis());
        sendbyte.write( ByteConverter.longTobyte( System.currentTimeMillis() ), 8 );
        NLog.d( "[ProtocolParser20] REQ buildSetCurrentTimeData." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build auto shutdown time setup byte [ ].
     *
     * @param shutdownTime the shutdown time
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x02 REQ_PenStatusChange_TYPE_AutoShutdownTime
    public static byte[] buildAutoShutdownTimeSetup ( short shutdownTime )
    {
        PacketBuilder sendbyte = new PacketBuilder( 3 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_AutoShutdownTime ), 1 );
        sendbyte.write( ByteConverter.shortTobyte( shutdownTime ), 2 );
        NLog.d( "[ProtocolParser20] REQ buildAutoShutdownTimeSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen cap on off setup byte [ ].
     *
     * @param on the on
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x03 REQ_PenStatusChange_TYPE_PenCapOnOff
    public static byte[] buildPenCapOnOffSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_PenCapOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );

        NLog.d( "[ProtocolParser20] REQ buildPenCapOnOffSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen auto power setup byte [ ].
     *
     * @param on the on
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x04 REQ_PenStatusChange_TYPE_AutoPowerOnSet
    public static byte[] buildPenAutoPowerSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_AutoPowerOnSet ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );

        NLog.d( "[ProtocolParser20] REQ buildPenAutoPowerSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen beep setup byte [ ].
     *
     * @param on the on
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x05 REQ_PenStatusChange_TYPE_BeepOnOff
    public static byte[] buildPenBeepSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_BeepOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );
        NLog.d( "[ProtocolParser20] REQ buildPenBeepSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen hover setup byte [ ].
     *
     * @param on the on
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x06 REQ_PenStatusChange_TYPE_HoverOnOff
    public static byte[] buildPenHoverSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_HoverOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );
        NLog.d( "[ProtocolParser20] REQ buildPenHoverSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen offline data save setup byte [ ].
     *
     * @param on the on
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x07 REQ_PenStatusChange_TYPE_OfflineDataSaveOnOff
    public static byte[] buildPenOfflineDataSaveSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_OfflineDataSaveOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );
        NLog.d( "[ProtocolParser20] REQ buildPenOfflineDataSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen tip color setup byte [ ].
     *
     * @param color the color
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x08 REQ_PenStatusChange_TYPE_LEDColorSet
    public static byte[] buildPenTipColorSetup ( int color )
    {
        byte[] cbyte = ByteConverter.intTobyte( color );


        PacketBuilder sendbyte = new PacketBuilder( 5 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_LEDColorSet ), 1 );
        //a
        sendbyte.write( cbyte[3] );
        //r
        sendbyte.write( cbyte[2] );
        //g
        sendbyte.write( cbyte[1] );
        //b
        sendbyte.write( cbyte[0] );

        NLog.d( "[ProtocolParser20] REQ buildPenTipColorSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen sensitivity setup byte [ ].
     *
     * @param sensitivity the sensitivity
     * @return the byte [ ]
     */
// 0x04 CMD20.REQ_PenStatus  0x09 REQ_PenStatusChange_TYPE_SensitivitySet
    public static byte[] buildPenSensitivitySetup ( short sensitivity )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_SensitivitySet ), 1 );
        sendbyte.write( ByteConverter.shortTobyte( sensitivity ), 1 );
        NLog.d( "[ProtocolParser20] REQ buildPenSensitivitySetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }



//    public static final int USING_NOTE_TYPE_NOTE = 1;
//    public static final int USING_NOTE_TYPE_SECTION_OWNER = 2;
//    public static final int USING_NOTE_TYPE_ALL = 3;

    private static byte[] buildAddUsingNotes ( int sectionId, int ownerId, int[] noteIds )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );

        PacketBuilder sendbyte = new PacketBuilder( 2 + noteIds.length *8 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) noteIds.length ));

        for ( int noteId : noteIds )
        {
            sendbyte.write( ownerByte[0] );
            sendbyte.write( ownerByte[1] );
            sendbyte.write( ownerByte[2] );
            sendbyte.write( (byte) sectionId );
            sendbyte.write( ByteConverter.intTobyte( noteId ) );
        }

        NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes.sectionId+"+sectionId+";ownerId="+ownerId+";noteIds len="+noteIds.length+ "Packet:" + sendbyte.showPacket());
        return sendbyte.getPacket();
    }

    /**
     * Build add using notes byte [ ].
     *
     * @param noteList the note list
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingNotes ( ArrayList<UseNoteData> noteList )
    {
        int setCount = 0;
        for(UseNoteData data : noteList)
        {
            if(data.ownerId != -1 && data.sectionId != -1 )
            {
                if(data.noteIds == null)
                    setCount++;
                else
                    setCount += data.noteIds.length;
            }
        }
        PacketBuilder sendbyte = new PacketBuilder( 2 + setCount *8 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) setCount ) );
        for(UseNoteData data : noteList)
        {
            if(data.ownerId != -1 && data.sectionId != -1 )
            {
                byte[] ownerByte = ByteConverter.intTobyte( data.ownerId );
                byte sectionIdByte = (byte)data.sectionId;
                if(data.noteIds == null)
                {
                    sendbyte.write( ownerByte[0] );
                    sendbyte.write( ownerByte[1] );
                    sendbyte.write( ownerByte[2] );
                    sendbyte.write( (byte) sectionIdByte );
                    sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );
                }
                else
                {
                    for(int noteId: data.noteIds)
                    {
                        sendbyte.write( ownerByte[0] );
                        sendbyte.write( ownerByte[1] );
                        sendbyte.write( ownerByte[2] );
                        sendbyte.write( (byte) sectionIdByte );
                        sendbyte.write( ByteConverter.intTobyte( noteId ) );
                    }
                }
            }
        }
        NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes ( ArrayList<UseNoteData> noteList )+" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build add using notes byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingNotes ( int sectionId, int ownerId )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );

        PacketBuilder sendbyte = new PacketBuilder( 2 + 4 + 4 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) 1 ) );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );

        NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes.sectionId+" + sectionId + ";ownerId=" + ownerId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build add using notes byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingNotes ( int[] sectionId, int[] ownerId )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 +  2 + sectionId.length *8 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) sectionId.length ) );
        for(int i = 0; i < sectionId.length; i++)
        {
            byte[] ownerByte = ByteConverter.intTobyte( ownerId[i] );
            sendbyte.write( ownerByte[0] );
            sendbyte.write( ownerByte[1] );
            sendbyte.write( ownerByte[2] );
            sendbyte.write( (byte) (sectionId[i]) );
            sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );
            NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes.sectionId+" + sectionId[i] + ";ownerId=" + ownerId[i] + "Packet:" + sendbyte.showPacket() );
        }

        return sendbyte.getPacket();
    }

    /**
     * Build add using all notes byte [ ].
     *
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingAllNotes ()
    {
       PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) 0xffff ) );
        NLog.d( "[ProtocolParser20] REQ buildAddUsingAllNotes" + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data list all byte [ ].
     *
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataListAll ()
    {
        PacketBuilder sendbyte = new PacketBuilder( 4 );
        sendbyte.setCommand( CMD20.REQ_OfflineNoteList );
        sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );

        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataListAll" + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data list byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataList (int sectionId, int ownerId)
    {
        PacketBuilder sendbyte = new PacketBuilder( 4 );
        sendbyte.setCommand( CMD20.REQ_OfflineNoteList );
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );

        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataList sectionId=" + sectionId + ";ownerId=" + ownerId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data page list byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataPageList (int sectionId, int ownerId, int noteId)
    {
        PacketBuilder sendbyte = new PacketBuilder( 8 );
        sendbyte.setCommand( CMD20.REQ_OfflinePageList );
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataPageList sectionId=" + sectionId + ";ownerId=" + ownerId + ";noteId=" + noteId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineData ( int sectionId, int ownerId, int noteId )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );

        PacketBuilder sendbyte = new PacketBuilder( 14 );
        sendbyte.setCommand( CMD20.REQ_OfflineDataRequest );
        // 오프라인 데이터 전송여부 , 전송한 데이터 삭제 : 1 , 안함 2
        sendbyte.write( (byte) 1 );

        // 데이터 압축 여부  1:압축  0:압축안함
        sendbyte.write( (byte) 1 );

        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );
        //페이지 개수 0 이면 노트 전체
        sendbyte.write( ByteConverter.intTobyte( 0 ) );

        NLog.d( "[ProtocolParser20] REQ buildReqOfflineData sectionId=" + sectionId + ";ownerId=" + ownerId + ";noteId=" + noteId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        int pageCount = 0;
        if(pageIds != null)
        {
            pageCount = pageIds.length;
        }

        PacketBuilder sendbyte = new PacketBuilder( 14 + pageCount * 4);
        sendbyte.setCommand( CMD20.REQ_OfflineDataRequest );
        // 오프라인 데이터 전송여부 , 전송한 데이터 삭제안함
        sendbyte.write( (byte) 1 );

        // 데이터 압축 여부  1:압축  0:압축안함
        sendbyte.write( (byte) 1 );

        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );

        //페이지 개수 0 이면 노트 전체
        sendbyte.write( ByteConverter.intTobyte( pageCount ) );
        if(pageCount != 0)
        {
            for(int pageId: pageIds)
                sendbyte.write( ByteConverter.intTobyte( pageId ) );
        }
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineData sectionId=" + sectionId + ";ownerId=" + ownerId + ";noteId=" + noteId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build offline chunk response byte [ ].
     *
     * @param errorCode the error code
     * @param packetId  the packet id
     * @param position  the position
     * @return the byte [ ]
     */
// 0x44 CMD20.P_OfflineChunkResponse
    public static byte[] buildOfflineChunkResponse ( int errorCode, int packetId, int position )
    {
        PacketBuilder builder = new PacketBuilder( 3,  errorCode);
        builder.setCommand( CMD20.ACK_OfflineChunk );
        builder.write( ByteConverter.intTobyte( packetId ), 2 );
        int isContinue = 0;
        if(position == 2)
            isContinue = 0;
        else
            isContinue = 1;
        builder.write( ByteConverter.intTobyte( isContinue ),1);

        NLog.d( "[ProtocolParser20] REQ buildOfflineChunkResponse :" + builder.showPacket() );
        return builder.getPacket();
    }

    /**
     * Build req offline data remove byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteIds   the note ids
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataRemove ( int sectionId, int ownerId, int[] noteIds)
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        int noteCount = 0;
        if(noteIds != null)
        {
            noteCount = noteIds.length;
        }

        PacketBuilder sendbyte = new PacketBuilder( 5 + noteCount *4 );
        sendbyte.setCommand( CMD20.REQ_OfflineNoteRemove );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( (byte) noteCount );
        if(noteCount != 0)
        {
            for(int noteId: noteIds)
                sendbyte.write( ByteConverter.intTobyte( noteId ) );
        }
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataRemove :" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }


//    여기부터

    /**
     * Build pen sw upgrade byte [ ].
     *
     * @param fwVersion  the fw version
     * @param deviceName the device name
     * @param filesize   the filesize
     * @param checkSum   the check sum
     * @return the byte [ ]
     */
// 0x31 CMD20.REQ_PenFWUpgrade
    public static byte[] buildPenSwUpgrade ( String fwVersion, String deviceName, int filesize, byte checkSum, boolean isCompress, int packetSize)
    {
        PacketBuilder builder = new PacketBuilder( 16 + 16 + 4 + 4 + 1 + 1 );

        builder.setCommand( CMD20.REQ_PenFWUpgrade );
        builder.write( ByteConverter.stringTobyte( deviceName ), 16 );
        builder.write( ByteConverter.stringTobyte( fwVersion ),16 );
        builder.write( ByteConverter.intTobyte( filesize ) );
        builder.write( ByteConverter.intTobyte( packetSize ) );


        // 압축 여부 1:압축 , 0: 압축안함
        if(isCompress)
        {
            builder.write( (byte) 1 );
        }
        else
            builder.write( (byte) 0 );
        builder.write( checkSum );
        NLog.d( "[ProtocolParser20] REQ buildPenSwUpgrade deviceName="+deviceName+":" + builder.showPacket() );
        return builder.getPacket();
    }

    /**
     * Build pen sw upload chunk byte [ ].
     *
     * @param offset     the offset
     * @param data       the data
     * @param status     the status
     * @param isCompress the is compress
     * @return the byte [ ]
     * @throws IOException the io exception
     */
// 0xB2 ACK_UploadPenFWChunk
    public static byte[] buildPenSwUploadChunk( int offset, byte[] data ,int status, boolean isCompress) throws IOException
    {
//        status 가 에러면 에러코드까지만 보냄
        if(status == CommProcessor20.FwPacketInfo.STATUS_ERROR)
        {
            PacketBuilder sendbyte = new PacketBuilder(0, 1 );
            sendbyte.setCommand( CMD20.ACK_UploadPenFWChunk );
            NLog.d( "[ProtocolParser20] REQ buildPenSwUploadChunk ERR :" + sendbyte.showPacket());
            return sendbyte.getPacket();

        }
        else
        {
            int beforeCompressSize = data.length;
            byte[] compressData = null;
            int afterCompressSize = 0;
            if(isCompress)
            {
                compressData = compress( data );
                afterCompressSize = compressData.length;
            }
            else
            {
                compressData = data;
                afterCompressSize = beforeCompressSize;
            }


            PacketBuilder sendbyte = new PacketBuilder( 1 + 4 + 1 + 4 + 4 + afterCompressSize, 0 );
            sendbyte.setCommand( CMD20.ACK_UploadPenFWChunk );
            sendbyte.write( (byte) 0 );
            sendbyte.write( ByteConverter.intTobyte( offset ) );
            sendbyte.write( Chunk.calcChecksum( data ) );
            sendbyte.write( ByteConverter.intTobyte( beforeCompressSize ) );
            sendbyte.write( ByteConverter.intTobyte(  afterCompressSize ) );
            sendbyte.write( compressData );
            NLog.d( "[ProtocolParser20] REQ buildPenSwUploadChunk beforeCompressSize:"+beforeCompressSize+",afterCompressSize:"+afterCompressSize+"," + sendbyte.showPacket() );
            return sendbyte.getPacket();

        }
    }


    static private byte[] compress(byte[] source) throws IOException
    {
//        ByteArrayInputStream in = new ByteArrayInputStream(source);
//        ByteArrayOutputStream bous = new ByteArrayOutputStream();
//        DeflaterOutputStream out =	new DeflaterOutputStream(bous);
//        byte[] buffer = new byte[1024];
//        int len;
//        while((len = in.read(buffer)) > 0) {
//            out.write(buffer, 0, len);
//        }
//        byte[] ret = bous.toByteArray();
//
//        bous.flush();
//        in.close();
//        out.close();
//        return ret;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED);
        DeflaterOutputStream deflaterStream = new DeflaterOutputStream(baos, deflater);
        deflaterStream.write(source);
        deflaterStream.finish();

        return baos.toByteArray();
    }

    /**
     * The interface Parsed packet listener.
     */
    public interface IParsedPacketListener
    {
        /**
         * On create packet.
         *
         * @param packet the packet
         */
        public void onCreatePacket ( Packet packet );
    }

    /**
     * The type Packet builder.
     */
    public static class PacketBuilder
    {
        private ByteBuffer escapeBuffer = ByteBuffer.allocate( buffer_size );
        private int afterEscapeSize = 0;
        /**
         * The Packet.
         */
        byte[] packet;
        /**
         * The Ret packet.
         */
        byte[] retPacket;
        /**
         * The Total length.
         */
        int totalLength, /**
     * The Data length.
     */
    dataLength;
        /**
         * The Position.
         */
        int position = 4;
        /**
         * The Is escape.
         */
        boolean isEscape = false;

        /**
         * Instantiates a new Packet builder.
         *
         * @param length the length
         */
        public PacketBuilder ( int length )
        {
            allocate( length );
        }

        /**
         * Instantiates a new Packet builder.
         *
         * @param length    the length
         * @param errorCode the error code
         */
        public PacketBuilder ( int length, int errorCode )
        {
            allocate( length , errorCode);
        }

        /**
         * write command field
         *
         * @param cmd the cmd
         */
        public void setCommand ( int cmd )
        {
            packet[1] = (byte) cmd;
        }

        /**
         * buffer allocation and set packet frame
         *
         * @param length the length
         */
        public void allocate ( int length  )
        {
            totalLength = length + 5;

            dataLength = length;

            position = 4;

            packet = new byte[this.totalLength];

            Arrays.fill( packet, (byte) PKT_EMPTY );

            packet[0] = (byte) PKT_START;
            byte[] bLength = ByteConverter.shortTobyte( (short) length );

            packet[2] = bLength[0];
            packet[3] = bLength[1];

            packet[totalLength - 1] = (byte) PKT_END;
        }

        /**
         * buffer allocation and set packet frame
         *
         * @param length    the length
         * @param errorCode the error code
         */
        public void allocate ( int length, int errorCode  )
        {
            totalLength = length + 6;

            dataLength = length;

            position = 5;

            packet = new byte[this.totalLength];

            Arrays.fill( packet, (byte) PKT_EMPTY );

            packet[0] = (byte) PKT_START;
            byte[] bLength = ByteConverter.shortTobyte( (short) length );

            packet[2] = (byte) errorCode;
            packet[3] = bLength[0];
            packet[4] = bLength[1];

            packet[totalLength - 1] = (byte) PKT_END;
        }

        /**
         * write data to data field
         *
         * @param buffer the buffer
         */
        public void write ( byte[] buffer )
        {
            for ( int i = 0; i < buffer.length; i++ )
            {
                packet[position++] = buffer[i];
            }
        }

        /**
         * write data to data field (resize)
         *
         * @param buffer     the buffer
         * @param valid_size the valid size
         */
        public void write ( byte[] buffer, int valid_size )
        {
            buffer = ResizeByteArray( buffer, valid_size );
            this.write( buffer );
        }

        /**
         * write single data to data field
         *
         * @param data the data
         */
        public void write ( byte data )
        {
            packet[position++] = data;
        }

        /**
         * Resize byte array byte [ ].
         *
         * @param bytes   the bytes
         * @param newsize the newsize
         * @return the byte [ ]
         */
        public byte[] ResizeByteArray ( byte[] bytes, int newsize )
        {
            byte[] result = new byte[newsize];
            Arrays.fill( result, (byte) 0x00 );

            int length = newsize > bytes.length ? bytes.length : newsize;

            for ( int i = 0; i < length; i++ )
            {
                result[i] = bytes[i];
            }

            return result;
        }

        /**
         * Get packet byte [ ].
         *
         * @return the byte [ ]
         */
        public byte[] getPacket ()
        {
            if(!isEscape)
            {
                escapeBuffer.clear();
                afterEscapeSize = 0;
                for(int i = 0; i <  packet.length ; i++)
                {
                    boolean escape = false;
                    if(i != 0 && i != packet.length -1)
                    {
                        if(packet[i] == (byte)PKT_START || packet[i] == (byte)PKT_DLE || packet[i] == (byte)PKT_END)
                        {
                            escapeBuffer.put( (byte)PKT_DLE);
                            afterEscapeSize++;
                            escape = true;
                        }
                    }
                    if(escape)
                        escapeBuffer.put( escapeData( packet[i]));
                    else
                        escapeBuffer.put( packet[i]);
                    afterEscapeSize++;
                }
                retPacket = Packet.copyOfRange(escapeBuffer.array(), 0, afterEscapeSize);
                isEscape = true;
            }
            return retPacket;
        }
        private byte escapeData ( byte source )
        {
            return (byte) ( source ^ PKT_ESCAPE );
        }

        /**
         * Show packet string.
         *
         * @return the string
         */
        public String showPacket ()
        {
            if(!isEscape)
            {
                escapeBuffer.clear();
                afterEscapeSize = 0;

                for(int i = 0; i <  packet.length ; i++)
                {
                    boolean escape = false;
                    if(i != 0 && i != packet.length -1)
                    {
                        if(packet[i] == (byte)PKT_START || packet[i] == (byte)PKT_DLE || packet[i] == (byte)PKT_END)
                        {
                            escapeBuffer.put( (byte)PKT_DLE);
                            afterEscapeSize++;
                            escape = true;
                        }
                    }
                    if(escape)
                        escapeBuffer.put( escapeData( packet[i]));
                    else
                        escapeBuffer.put( packet[i]);
                    afterEscapeSize++;
                }
                retPacket = Packet.copyOfRange(escapeBuffer.array(), 0, afterEscapeSize);
            }
            StringBuffer buff = new StringBuffer();

            for ( byte item : retPacket )
            {
                int int_data = (int) ( item & 0xFF );
                buff.append( Integer.toHexString( int_data ) + ", " );
            }

//            NLog.d( "[PacketBuilder] showPacket : " + buff.toString() );

            String ret = buff.toString();
            buff = null;
            return ret;
        }

        /**
         * Show packet string.
         *
         * @param bytes the bytes
         * @param count the count
         * @return the string
         */
        public static String showPacket ( byte[] bytes , int count)
        {
            StringBuffer buff = new StringBuffer();

            for ( int i = 0; i < count; i++ )
            {
                byte item = bytes[i];
                int int_data = (int) ( item & 0xFF );
                buff.append( Integer.toHexString( int_data ) + ", " );
            }

//            NLog.d( "[PacketBuilder] showPacket : " + buff.toString() );

            String ret = buff.toString();
            buff = null;
            return ret;
        }
    }
}
