 function WebsocketController()
 {
    this.supported=false;
    this.open=false;
    this.ws=null;
    this.guideUpdateTimer=null;
    var id;
    var stations;
    var epg;
    var that=this;

    if ("WebSocket" in window)
    {
            console.log("WebSocket is supported by your Browser!");
            this.supported=true;
            initialize();

    }else
    {
       console.log("WebSocket NOT supported by your Browser!");
    }

    this.send=function(message, epgUpdate){
        if (udpdateEPGinProgress && epgUpdate ){
            console.log("Cannot request epg update until existing completed: ");
        }
        if (null!=ws){
            if (epgUpdate)
                udpdateEPGinProgress=true;
            console.log(" Sending message: " + message);
            ws.send(message);
        }
        else{
            console.log("No websocket to send message: "+message);
        }
    };

    function initialize(){
       // Let us open a web socket
        var ip="ws://"+location.host;
        console.log("IP address: "+ip);
        ws= new WebSocket(ip);

            ws.onopen = function()
            {
                that.open=true;
                //setInterval (pingWebSocket, 2000);

                ws.send('keepUIVisible:120000', false);

            };

            ws.onmessage = function (evt)
            {
              var received_msg = evt.data;
              console.log("Message is received..." + received_msg);
              if (received_msg.startsWith("{") && received_msg.endsWith("}")){   //This is a JSON Object

                    var obj=JSON.parse(received_msg);
                    var keyChannel, keyDate, keyTime, keyMetadata, stations, keyStation;
                    for (key in obj){
                        console.log ("KEY: "+key);
                        if (key=="EPG"){
                            epg=obj[key];
                            for (keyChannel in epg){
                                console.log("channel:" + keyChannel);
                                var dateArray=epg[keyChannel];
                                for (keyDate in dateArray){
                                    console.log("  date: "+keyDate);
                                    var timeArray=dateArray[keyDate];
                                    for (keyTime in timeArray){
                                        console.log("      time: "+keyTime);
                                        var metadata=timeArray[keyTime];
                                        for (keyMetadata in metadata){
//                                            console.log("        "+keyMetadata +"->"+metadata[keyMetadata]);
                                        }
                                        if (null==EPG_DATA[keyChannel]){
                                            EPG_DATA[keyChannel]={};
                                        }
                                        if (null==EPG_DATA[keyChannel].metadata){

                                            EPG_DATA[keyChannel].metadata=[metadata];

                                        }
                                        if (metadata.start<EPG_DATA[keyChannel].metadata[0].start){
                                            epgDataAvailable=true;
                                            var l=EPG_DATA[keyChannel].metadata.length;
                                            for (var i=l; i>0; i--){

                                                EPG_DATA[keyChannel].metadata[i]=EPG_DATA[keyChannel].metadata[i-1];
                                            }
                                            EPG_DATA[keyChannel].metadata[0]=metadata;

                                        }else if (metadata.start>EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length-1].start){
                                            epgDataAvailable=true;
                                            EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length]=metadata;
                                        }
                                    }


                                    console.log("EPG_DATA length:"+EPG_DATA[keyChannel].metadata.length);
                                    var d=new Date(parseInt(EPG_DATA[keyChannel].metadata[0].start));
                                    var d2=new Date(parseInt(EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length-1].start));
                                    console.log("EPG_DATA start:"+d.toUTCString());
                                    console.log("EPG_DATA end:"+ d2.toUTCString());
                                }

                            }

                            udpdateEPGinProgress=false;
                            console.log("EPG_DATA: udpdateEPGinProgress=false" );


                        }else if (key=="STATIONS"){
                            var stationData;
                            stations= obj[key];
                            STATION_DATA=[];
                            stationDataAvailable=true;
                            for (keyStation in stations){
                                console.log(keyStation);
                                for (stationData in stations[keyStation]){
                                        console.log("   "+stationData+" -> " + stations[keyStation][stationData]);

                                }
                                var channel=new Object();
                                channel.name=stations[keyStation]["callSign"];
                                channel.icon_url = stations[keyStation]["channelIcon"];
                                channel.channelId = stations[keyStation]['channelID'];
                                channel.channelNumber=stations[keyStation]['channelNumber'];
                                channel.icon=new Image();
                                channel.icon.imageloaded=false;

                                var l=STATION_DATA.length;
//                                var newStation=true;
//                                for (var i=0; i<l; i++){
//                                    if (STATION_DATA[i].channelId == channel.channelId ){
//                                        newStation=false;
//                                        break;
//                                    }
//                                }
//                                if (STATION_DATA[l].channelId != channel.channelId ){

                                STATION_DATA[l]=channel;

                                console.log()
//                                }
//                                updateChannelList=false;
                            }

                            udpdateEPGinProgress=false;
                            console.log("STATION_DATA: udpdateEPGinProgress=false" );


                        }else if (key=="PROGRAMS"){
                            epg=obj[key];
                            var epglength=epg.length;
                            console.log("epglength: "+epglength);
                            for (var j=0; j<epglength; j++){


                                var metadata=epg[j];
                                keyChannel=metadata["channelId"];
                                console.log("channel:" + keyChannel);
                                if (null==keyChannel){

                                }
                                else
                                {
                                    if (null==EPG_DATA[keyChannel]){
                                        EPG_DATA[keyChannel]={};
                                    }


                                    if (null==EPG_DATA[keyChannel].metadata){

                                        EPG_DATA[keyChannel].metadata=[metadata];

                                    }else{
                                        if (metadata.start<EPG_DATA[keyChannel].metadata[0].start){
                                            epgDataAvailable=true;
                                            var l=EPG_DATA[keyChannel].metadata.length;
                                            for (var i=l; i>0; i--){
                                                EPG_DATA[keyChannel].metadata[i]=EPG_DATA[keyChannel].metadata[i-1];
                                            }
                                            EPG_DATA[keyChannel].metadata[0]=metadata;

                                        }else if (metadata.start>EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length-1].start){
                                            epgDataAvailable=true;
                                            EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length]=metadata;
//                                        }else{
//                                            for (var i=1; i<EPG_DATA[keyChannel].metadata.length-1; i++){
//                                                var start1=EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata[i]].start;
//                                                var end1=EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata[i].start +EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length;
//                                                var start2=EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata[i+1].start;
//                                                if (metadata.start)> start1){
//                                                   if (metadata.start < end1){
//                                                       epgDataAvailable=true;
//                                                       EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata[i]=metadata;
//                                                       break;
//                                                   }
//                                                 }
//
//                                            }

                                        }
                                    }


                                    console.log("EPG_DATA length:"+EPG_DATA[keyChannel].metadata.length);
                                    var d=new Date(parseInt(EPG_DATA[keyChannel].metadata[0].start));
                                    var d2=new Date(parseInt(EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length-1].start));
                                    console.log("EPG_DATA start:"+d.toUTCString());
                                    console.log("EPG_DATA end:"+ d2.toUTCString());
                                }

                            }

                            udpdateEPGinProgress=false;
                            console.log("EPG_DATA: udpdateEPGinProgress=false" );

                        } else if (key=="CURRENT"){
                            console.log("Found CURRENT key");
                            for (var i=0; i<STATION_DATA.length; i++){
                                STATION_DATA[i].playing=false;
                            }

                            for (var i=0; i<STATION_DATA.length; i++){
                                if (STATION_DATA[i].channelId == obj[key]){
                                    console.log("Current_playing: "+STATION_DATA[i].channelId);
                                    STATION_DATA[i].playing=true;
                                    break;
                                }
                            }
                        }else if (key == "FAVORITES" ){
                            var faveChannel= obj[key];
                            for (var i=0; i<STATION_DATA.length; i++){
                                STATION_DATA[i].favorite=false;
                            }
                            console.log("FAVORITES key length: "+faveChannel.length);

                            for (var j=0; j<faveChannel.length; j++){

                                for (var i=0; i<STATION_DATA.length; i++){
                                    if (STATION_DATA[i].channelId == faveChannel[j]){
                                        STATION_DATA[i].favorite=true;
                                        break;
                                    }
                                }
                            }

//                            favoritesAvailable=true;
                            udpdateEPGinProgress=false;

                        } else if (key =="CHANNELS"){
                            var channelList=obj[key];
                            console.log("CHANNELLIST_DATA length: "+channelList.length);
                            console.log(channelList[0]);
                            for (var i=0; i<channelList.length; i++){
                                for (var j=0; j<STATION_DATA.length; j++){
                                    if (STATION_DATA[j].channelId==channelList[i]){
                                        CHANNELLIST_DATA[i]=j;
//                                        console.log("CHANNELIST_DATA i: "+j);
                                        j=STATION_DATA.length;
                                    }
//                                    CHANNELLIST_DATA[i]=0;  //default error
//                                    console.log("Error CHANNELLIST_DATA: "+STATION_DATA[j].channelID +"  not found");
                                }

                            }
                        }


                    }
                }


            };


           ws.onclose = function()
           {
              // websocket is closed.
              console.log("Connection is closed...");

           };
    }

//    function pingWebSocket(){
//        ws.send("ping");
//        console.log("Ping    sent");
//    }




}

