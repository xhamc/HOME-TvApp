function GuideController(){


//GLOBALS:
	GRIDINTERVAL=30*60*1000;
	EPG_DATA={};					//Incoming EPG data object
	STATION_DATA=[];				//Incoming Station data
	CHANNELLIST_DATA=[];			//Converted/Sorted station data
	udpdateEPGinProgress=false;
	stationDataAvailable=false;
	epgDataAvailable=false;

//END GLOBALS
	var GUIDE_FETCH_REQUEST_TIMER=1000;
	var STATION_FETCH_REQUEST_INT=5;
	var GUIDE_WEBSOCKET_TIMER=200;


	var TIMELIST_DATA=[];
	var currentAvailableDataRange;			//tracks the start and end times of all the channels
	var updateTimeNext=true;				//flag to decide whether to update the data for current set of 5 channels or go to next set of channels
	var updateChannelStart=0				//Tracks the start to the group of channels to update next
	var wsopen=false;						//one time flag for opening the websocket;

    var ws=new WebsocketController(); if (!ws.supported) return;
    var gc=new GridController(ws);
	var originalGridTimeStart=gc.initialize();
	var MAXTTIME=10*24*3600*1000;	//start with 10 days of maximum data!

	guideWebsocketControllerTimer(); //TODO switch to callbacks for efficiency

	function guideWebsocketControllerTimer(){
		if (!wsopen && ws.open){
			console.log("open");
			stationDataFetchRequest();
			wsopen=true;
		}else if (stationDataAvailable){
			console.log("sort");
			sortChannelListData();
			stationDataAvailable=false;
			guideDataFetchRequest();
		}else if (epgDataAvailable){
			console.log("epgData");
			epgDataAvailable=false;
			guideDataReceived();
		}
		setTimeout(guideWebsocketControllerTimer,GUIDE_WEBSOCKET_TIMER);
	}

	function sortChannelListData(){

		//initialize the channelist data if needed.
		//TODO: use preferences to turn random STATION_DATA into customized STATION_DATA (could be done from Java side too!)
/****************************Sort STATION_DATA by ChannelNumber into CHANNELLIST_DATA****************/
//			for (var i=0; i<STATION_DATA.length;i++){
//				var channelNumber=parseInt(STATION_DATA[i].channelNumber);
//				if (CHANNELLIST_DATA.length>0){
//					if (parseInt(CHANNELLIST_DATA[CHANNELLIST_DATA.length-1].channelNumber) < channelNumber ){
//						CHANNELLIST_DATA[CHANNELLIST_DATA.length]=STATION_DATA[i];
//					}else{
//						var j=CHANNELLIST_DATA.length;
//						do{
//							j--;
//							CHANNELLIST_DATA[j+1]=CHANNELLIST_DATA[j];
//						} while (j>0 && parseInt(CHANNELLIST_DATA[j-1].channelNumber) >= channelNumber)
//
//						CHANNELLIST_DATA[j]=STATION_DATA[i];
//
//					}
//
//
//				}else{
//					CHANNELLIST_DATA[0]=STATION_DATA[i];
//				}
//			}
/****************************Sort STATION_DATA by ChannelID into CHANNELLIST_DATA****************/
//			for (var i=0; i<STATION_DATA.length;i++){
//				var channelNumber=parseInt(STATION_DATA[i].channelId);
//				if (CHANNELLIST_DATA.length>0){
//					if (parseInt(CHANNELLIST_DATA[CHANNELLIST_DATA.length-1].channelId) < channelNumber ){
//						CHANNELLIST_DATA[CHANNELLIST_DATA.length]=STATION_DATA[i];
//					}else{
//						var j=CHANNELLIST_DATA.length;
//						do{
//							j--;
//							CHANNELLIST_DATA[j+1]=CHANNELLIST_DATA[j];
//
//						} while (j>0 && parseInt(CHANNELLIST_DATA[j-1].channelId) >= channelNumber)
//						CHANNELLIST_DATA[j]=STATION_DATA[i];
//
//					}
//				}else{
//					CHANNELLIST_DATA[0]=STATION_DATA[i];
//				}
//
//			}
/****************************Don't Sort STATION_DATA ****************/
			console.log("Sorting the channel data");
			for (var i=0; i<STATION_DATA.length;i++){
				CHANNELLIST_DATA[i]=STATION_DATA[i];
			}

			if (null==currentAvailableDataRange){
				currentAvailableDataRange=new Array(CHANNELLIST_DATA.length);
				for (var i=0; i<CHANNELLIST_DATA.length; i++){
						currentAvailableDataRange[i]={"firstProgramStart":originalGridTimeStart, "endProgramStart":originalGridTimeStart}; //initialize
				}
			}

//
//			for (var i=0; i<CHANNELLIST_DATA.length; i++) { console.log(CHANNELLIST_DATA[i].channelId); }
////			lastChannelAvailable=CHANNELLIST_DATA.length-1;
	}

	function guideDataReceived(){


		for (var i=0; i<CHANNELLIST_DATA.length; i++){
			if (null!=EPG_DATA[CHANNELLIST_DATA[i].channelId]){
				var m=EPG_DATA[CHANNELLIST_DATA[i].channelId].metadata;
				var l=m.length;

				if (null!=currentAvailableDataRange[i]){
					var start= Math.max(m[0].start, currentAvailableDataRange[i].firstProgramStart);
					var end= Math.max(m[l-1].start, currentAvailableDataRange[i].endProgramStart );

				}else{
					var start =parseInt(m[0].start);
					var end= parseInt(m[l-1].start);
				}
				currentAvailableDataRange[i]={"firstProgramStart":start, "endProgramStart":end};
			}
		}
	}

	function stationDataFetchRequest(){
			console.log("Fetching station data");
			if (!udpdateEPGinProgress){
    			if (CHANNELLIST_DATA.length==0){
    				ws.send("browseEPGStations", true);
    			}
			}
	}

	var getStations=STATION_FETCH_REQUEST_INT;
	function guideDataFetchRequest(){

		console.log("Fetching guide data");

		if (!udpdateEPGinProgress){
			getStations--;
			if (getStations==0){
				getStations=STATION_FETCH_REQUEST_INT;
				ws.send("browseEPGStations", true);
				return;
			}else{
				var updateRequest=getNextTimeAndChannelList();
				if (null!=updateRequest){
					var msg=JSON.stringify({"browseEPGData":updateRequest});
					console.log("msg:"+msg);
					ws.send(msg, true);
				}
			}

		}

		setTimeout(guideDataFetchRequest,GUIDE_FETCH_REQUEST_TIMER);
	}




	var maxTimeLast=originalGridTimeStart+GRIDINTERVAL*10;
	function getNextTimeAndChannelList(){
		var channelData=[];
		var timeData=[];
		console.log("updateTimeNext: "+updateTimeNext);
		var mxt=getTime().ms+MAXTTIME;
		if (maxTimeLast>(getTime().ms+MAXTTIME)){
			return null;
		}

		var numChannels=Math.min(5, (currentAvailableDataRange.length-updateChannelStart));
		var start=mxt;
		for (var i=0; i<numChannels; i++){
			if (currentAvailableDataRange[i+updateChannelStart].endProgramStart<start){
				start=parseInt( currentAvailableDataRange[i+updateChannelStart].endProgramStart );
			}
			channelData[i]=CHANNELLIST_DATA[i+updateChannelStart].channelId;

		}
		var end=maxTimeLast;
		timeData[0]=start;
		timeData[1]=end;


		if ((updateChannelStart+5)>=currentAvailableDataRange.length){
			updateChannelStart=0;
			maxTimeLast+=GRIDINTERVAL*10;
		}else{
			updateChannelStart+=5;
		}
//
//		if (updateTimeNext){
//			maxTimeLast=end;
//			if ((updateChannelStart+5)<currentAvailableDataRange.length && maxTimeLast<=MAXTTIME){
//				updateTimeNext=!updateTimeNext;
//				updateChannelStart+=5;
//			}else if (maxTimeLast>MAXTTIME){
//				return null;		//no more data. return null stops any more requests.
//			}
//		}else{
//
//			if (end>=maxTimeLast) {
//				updateTimeNext=!updateTimeNext;
//				if ((updateChannelStart+5)<currentAvailableDataRange.length){
//					updateChannelStart+=5;
//				}else{
//					updateChannelStart=0;
//				}
//			}
//		}
		var jo={"CHANNELLIST":channelData, "TIMELIST":timeData};

		return jo;


	}


}