function GridController(){

	var GRIDINTERVAL=30*60*1000;
	var VERTCELLSIZE=(1070-160)/10;
	var HORIZCELLSIZE=(1905-300)/5;

	GRID_UPDATE_TIMER=100;
	GRID_TIME_START_TIMER=10*1000;

    var ctx;
	var background;
	var width;
	var height;
    var gridUpdateTimer;
    var gridTimeStart=0;
    var lastGridPoint=0;
    var midSelectionPoint;
	var currentChannelGridOffset=0;
    var offScreenLastGridPoint=0;
    var currentProgramSelected;
    var originalGridTimeStart;


    this.initialize=function (){
        canvas = document.getElementById('canvas');
        window.addEventListener("keypress",keyDown, false);
        width = canvas.width;
        height = canvas.height;
        ctx = canvas.getContext('2d');
        ctx.font = "30px arial";
        background = new Image();
//        mouse = new Object(); mouse.x = 0; mouse.y = 0; mouse.flag = 0; selected_program = null;
        background.src = 'img/background.png';

        gridTimeStartUpdate();
//        var now=getTime();
//        gridTimeStart= Math.floor(now.ms/GRIDINTERVAL)*GRIDINTERVAL - GRIDINTERVAL*3;
//        lastGridPoint=gridTimeStart+GRIDINTERVAL*10;
        originalGridTimeStart=gridTimeStart;
        gridUpdateTimer=setInterval(grid,GRID_UPDATE_TIMER);
        return originalGridTimeStart;
    }


	function grid(){



		ctx.save();
		//Draw background
		ctx.clearRect(0,0, width, height);
		ctx.globalAlpha=0.7;
		// ctx.drawImage(background, 0, 0, width, height);
		//Draw TODAY DATE
		ctx.globalAlpha=1;
		ctx.font = "50px arial";
		ctx.fillStyle='#FFFFFF';
		var now=getTime();
		ctx.fillText ('TODAY  |   '+ now.date+ '    '+now.time, 120,80);


		//Draw OUTER RECT
		ctx.strokeStyle='#FFFFFF';
		ctx.globalAlpha=0.5;
		ctx.lineWidth=2;
		ctx.strokeRect(5,5,1905,1065);
		//Draw GRID OUTLINE
		ctx.globalAlpha=0.1;
		ctx.beginPath();
		ctx.moveTo(5,160);
		ctx.lineTo(1910,160);
		for (var i=0; i<5; i++){
				ctx.moveTo(300+i*HORIZCELLSIZE,160);
        		ctx.lineTo(300+i*HORIZCELLSIZE,1070);
		}

			ctx.moveTo(5,160+VERTCELLSIZE);
			ctx.lineTo(1905,160+VERTCELLSIZE);

		for (var i=2; i<10; i++){
			ctx.moveTo(300-10,160+i*VERTCELLSIZE);
			ctx.lineTo(300,160+i*VERTCELLSIZE);

		}
		ctx.stroke();

		//Draw TIMES
		ctx.globalAlpha=1;
		ctx.font = "40px arial";
		ctx.textBaseline="middle";

		for (var i=2; i<10; i++){

			var displayTimeOnGrid=getTime(gridTimeStart+i*GRIDINTERVAL);

			ctx.fillText (displayTimeOnGrid.time, 80, 160+i*VERTCELLSIZE);
			if (displayTimeOnGrid.time=="0:00" || displayTimeOnGrid.time=="12:00a"){
				ctx.save();
				ctx.strokeStyle='#00FF00';
				ctx.globalAlpha=0.3;
				ctx.beginPath();
				ctx.moveTo(0, 160+i*VERTCELLSIZE);
				ctx.lineTo(300, 160+i*VERTCELLSIZE);
				ctx.stroke();
				ctx.restore();
			}


		}

		displayTimeOnGrid=getTime(gridTimeStart+GRIDINTERVAL);		//30 minutes of the visible data is hidden so date corresponds to 30 mins  beyond
		var date=displayTimeOnGrid.date.substring(0,displayTimeOnGrid.date.lastIndexOf(' '));
		ctx.fillText (date, 80, 120 + VERTCELLSIZE);





		ctx.strokeStyle='#FF0000';
		ctx.globalAlpha=0.75;
		ctx.beginPath();

		var nowGridOffset= (getTime().ms - getTime(gridTimeStart).ms)* VERTCELLSIZE /GRIDINTERVAL;

		if (nowGridOffset>VERTCELLSIZE){			//display starts one cell past the gridstart
			ctx.moveTo(300, 160 + nowGridOffset);
			ctx.lineTo(1905, 160 + nowGridOffset);
			ctx.stroke();
		}
		ctx.restore();
		drawPrograms();

	}

	function drawPrograms(){
		ctx.save();
		if (null!=CHANNELLIST_DATA ){
			ctx.font = "50px arial";
			ctx.fillStyle='#FFFFFF';

			var gridChannelMax=Math.min(currentChannelGridOffset+5, CHANNELLIST_DATA.length);
			for (var i=currentChannelGridOffset; i<gridChannelMax; i++){

				drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+100, 120+VERTCELLSIZE, HORIZCELLSIZE-110, CHANNELLIST_DATA[i].name, 0, 2, 1 );
//				ctx.fillText (CHANNELLIST_DATA[i].name, 300+(i-currentChannelGridOffset)*HORIZCELLSIZE+100, 140+VERTCELLSIZE);

				if (isURL(CHANNELLIST_DATA[i].icon_url) && !CHANNELLIST_DATA[i].icon.imageloaded){
					CHANNELLIST_DATA[i].icon.onload=function(){
						this.imageloaded=true;
					};
					CHANNELLIST_DATA[i].icon.src=CHANNELLIST_DATA[i].icon_url;
				}


				if (CHANNELLIST_DATA[i].icon.imageloaded){
					ctx.globalAlpha=0.2;
					ctx.fillRect(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,160 + VERTCELLSIZE-10-75, 75,75);
					ctx.globalAlpha=1;
					ctx.drawImage(CHANNELLIST_DATA[i].icon, 300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10, 160 + VERTCELLSIZE-10-75, 75,75);
				}
			}

			if (null!=EPG_DATA){

				for (var i=currentChannelGridOffset; i<gridChannelMax; i++){

					if (null!=EPG_DATA[CHANNELLIST_DATA[i].channelId]){

						var j=0;
						var m=EPG_DATA[CHANNELLIST_DATA[i].channelId].metadata;
						while (j<m.length){
							var start =parseInt(m[j].start);
							var end =parseInt(m[j].start) + parseInt(m[j].length);
							if (end>gridTimeStart) break;
							j++;
						}

						lastGridPoint=gridTimeStart+GRIDINTERVAL*10;

						ctx.fillStyle='#0000FF';
						ctx.strokeStyle='#FFFFFF';
						ctx.lineWidth=1;
						ctx.globalAlpha=0.2;
						ctx.fillStyle='#7F7F7F';

						while (j<m.length )
						{

							var start =parseInt(m[j].start);
							var end =parseInt(m[j].start) + parseInt(m[j].length);

							if (start<lastGridPoint && end>gridTimeStart){
								var truncated =false;
								if (start<(gridTimeStart+GRIDINTERVAL)){
									start = (gridTimeStart+GRIDINTERVAL);
									truncated=true;
								}
								if (end>lastGridPoint) {
									end = lastGridPoint;
									truncated=true;
								}
								var h= (end-start)*VERTCELLSIZE/GRIDINTERVAL;


								var y=160+ (start-gridTimeStart)*VERTCELLSIZE/GRIDINTERVAL;

								var now=getTime().ms;
								if (now>start && now < end){		//cell is current so shade
										ctx.fillRect(301+(i-currentChannelGridOffset)*HORIZCELLSIZE, y+1,HORIZCELLSIZE-2, h-2);
								}

								if (y>160 && y< lastGridPoint)			//no start line needed (could be before this anyway)
								{
									ctx.beginPath();
									ctx.moveTo(300+(i-currentChannelGridOffset)*HORIZCELLSIZE,y);
									ctx.lineTo(300+((i-currentChannelGridOffset)+1)*HORIZCELLSIZE,y);
									ctx.stroke();
								}

								var space=(end-start)*VERTCELLSIZE/GRIDINTERVAL;


								if (space>VERTCELLSIZE/4 && space<=VERTCELLSIZE/3 && !truncated){
									drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 3, 4, 1 );
								}else if (space>VERTCELLSIZE/3 && space<=VERTCELLSIZE/2 && !truncated){
									drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 2, 3, 1 );
								}else if (space>VERTCELLSIZE/2 && space<=VERTCELLSIZE){
									drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 1, 2, 1 );
								} else if (space>VERTCELLSIZE  && space<=VERTCELLSIZE*2){
									if (m[j].programTitle=="" ) {
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 1, 2, 1 );
									}else{
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/3, HORIZCELLSIZE-20 ,m[j].title, 1, 2, 1 );
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+2*space/3, HORIZCELLSIZE -20 ,m[j].programTitle, 2, 4, 1);
									}
								} else if (space>VERTCELLSIZE*2 && space<=VERTCELLSIZE*3 ){
									if (m[j].programTitle=="" ){
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 1, 2, 2 );
									}else{
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/3, HORIZCELLSIZE-20 ,m[j].title, 1, 2, 2);
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+2*space/3, HORIZCELLSIZE -20 ,m[j].programTitle, 2, 4, 2);
									}
								}
								else if (space>VERTCELLSIZE*3 ){
										if (m[j].programTitle=="" ){
											drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 1, 2, 4 );
										}else{
											drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/3, HORIZCELLSIZE-20 ,m[j].title, 1, 2, 3);
											drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+2*space/3, HORIZCELLSIZE -20 ,m[j].programTitle, 2, 3, 3);
										}
								}
							}

							j++;
						}
					}
				}


				if (null==currentProgramSelected){
					if (null!=EPG_DATA && null!=CHANNELLIST_DATA){
						if (null!=CHANNELLIST_DATA[currentChannelGridOffset]){
							if (null!=EPG_DATA[CHANNELLIST_DATA[currentChannelGridOffset].channelId]){
								if (null!=EPG_DATA[CHANNELLIST_DATA[currentChannelGridOffset].channelId].metadata){
									updateSelection(currentChannelGridOffset);
								}
							}
						}
					}

				} else{
					drawSelection();
				}

			}
			ctx.restore();


		}

	}


	function isURL(str) {
	  var pattern = new RegExp('^(https?:\\/\\/)?'+ // protocol
	  '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.?)+[a-z]{2,}|'+ // domain name
	  '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
	  '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
	  '(\\?[;&a-z\\d%_.~+=-]*)?'+ // query string
	  '(\\#[-a-z\\d_]*)?$','i'); // fragment locator
	  return pattern.test(str);
	}

	function drawTextInBox(x, y, w, t, largest, smallest, multiline){

		ctx.save();
		ctx.fillStyle='#FFFFFF';
		ctx.textBaseline="middle";
		ctx.globalAlpha=1.0;

		var fonts=["50px arial","40px arial","30px arial","25px arial","20px arial","15px arial", "12px arial", "10px arial", "8px arial"];
		var fontheights=[50.0,40.0,30.0,25.0,20.0,15.0,12.0,10.0,8.0];
		var dim;
		var left;
		var actualWidth;
		var i=largest;


		if (multiline==1){

				do{
        			ctx.font=fonts[i];
        			dim=ctx.measureText(t);
        			i++;
        			actualWidth=dim.width;
        			//console.log("font: "+ ctx.font + "  width: "+ actualWidth);
        		}while (actualWidth>w && i<=smallest )


        		if (actualWidth>w){
        			do{

        				t=t.substring(0,t.length - 3)+'..';
        				dim=ctx.measureText(t);
        				actualWidth=dim.width;

        			}while (actualWidth>w)
        		}


        		left =x+(w-actualWidth)/2;
        		ctx.fillText(t,left, y, w);


		}else{

			var phraseline;
			var phraselength;
			var numlines;
			var words=t.split(' ');
			var wordslength=new Array(words.length);
			var fontindex=0;

			do{
				ctx.font=fonts[fontindex];
				for (var j=0; j<words.length; j++){
					wordslength[j]=ctx.measureText(' '+words[j]).width;
//					console.log("words: " +words[j]+"  length: "+wordslength[j]);

				}
				phraseline=new Array(multiline);
				phraselength=new Array(multiline);
				for (var j=0; j<phraseline.length; j++){
					phraseline[j]='';
					phraselength[j]=0;
				}
				var phraseIndex=0;
				var nextWord=0;
				numlines=0;
				while (nextWord<words.length && numlines < multiline ){
					while ( (phraselength[numlines]+wordslength[nextWord])<w){
							phraseline[numlines]+=' '+words[nextWord];
							phraselength[numlines]+=phraselength[numlines]+wordslength[nextWord];
							nextWord++;
					}
					if (phraseline[numlines].endsWith(' '))  phraseline[numlines-1].substring (0,phraseline[numlines-1].length-1);
					numlines++;
				}

				fontindex++;

			}while (fontindex<=smallest && (numlines==multiline && nextWord<words.length) )

//			console.log(" font index: "+fontindex);
//			console.log(" font smalles: "+smallest);

			if (numlines==multiline && nextWord<words.length){
				phraseline[numlines-1]+='...';
			}
			for ( var j=0; j< numlines; j++){

//				console.log("phraseLines: " +phraseline[j])
				var actualWidth=ctx.measureText(phraseline[j]).width;
				var left =x+(w-actualWidth)/2;

//				console.log("fontheight: "+fontheights[fontindex]);
				var middle=y+j*fontheights[fontindex]*1.5 - numlines*fontheights[fontindex]*0.5;
//				console.log("left: "+left+"  middle: "+middle+"  width:  "+actualWidth);
				ctx.fillText(phraseline[j],left, middle, w);
			}
		}
		ctx.restore();
	}

	function drawSelection(){
		var s=getSelectionParameters();
		ctx.save();
		ctx.strokeStyle='#00FFFF';
		ctx.lineWidth=5;
		ctx.globalAlpha=0.5;

		ctx.strokeRect(s.x-5,s.y-5, HORIZCELLSIZE+10, s.h+10  );
		ctx.restore();
		if (descriptionBoxDrawFlag) drawDescriptionBox(s);
	}

	var descriptionImage=new Image();
	var lastURL="";
	function getDescriptionImage(url){
		if (lastURL!=url){
				console.log("getting image from "+url);
			lastURL=url;
			descriptionImage.src=url;
			descriptionImage.onload=function(){
				this.imageloaded=true;

				console.log("got image");

			};
		}
	}

	function drawDescriptionBox(selection){
	console.log("drawing selection box");
		var x;
		var y=160+VERTCELLSIZE*4+10;
		var h= VERTCELLSIZE*6.5 - 20;
		var w= HORIZCELLSIZE*3.5;
		if (selection.x> 300+ HORIZCELLSIZE ){
			x= selection.x-30-0.5*HORIZCELLSIZE;
		}else{
			x= selection.x+30
		}
		if (selection.y> 160+VERTCELLSIZE*4){
			y= selection.y-30-3.5*VERTCELLSIZE;
		}else{
			y=selection.y+30;
		}
		while((y+h)>1070){
			y-=0.5*VERTCELLSIZE;
		}
		while((x+w)>1910){
			x-=0.5*HORIZCELLSIZE;
		}

		ctx.save();
		ctx.clearRect(x,y,w,h);
		ctx.strokeStyle='#00FFFF';
		ctx.lineWidth=5;
		ctx.globalAlpha=0.5;
		ctx.strokeRect(x,y,w,h);


		ctx.globalAlpha=0.7;
		ctx.drawImage(background, x,y,w,h);
		//Draw TODAY DATE
		ctx.globalAlpha=1;
		ctx.font = "50px arial";
		ctx.fillStyle='#FFFFFF';

		var index=currentProgramSelected.item;
		channel=currentProgramSelected.channel;
		var m= EPG_DATA[CHANNELLIST_DATA[channel].channelId].metadata[index];
		if (isURL(m.programIcon)){
				getDescriptionImage(m.programIcon)
				if (descriptionImage.imageloaded){
					ctx.drawImage(descriptionImage, x+10, y+10, 300, 300);

				}
		}

		ctx.restore();
	}

	function getSelectionParameters(){

			if (null == CHANNELLIST_DATA || null==EPG_DATA ) return {"x":0,"y":0, "h":0, "mid":0};

			index=currentProgramSelected.item;
			channel=currentProgramSelected.channel;

			if (null==CHANNELLIST_DATA[channel]) return {"x":0,"y":0, "h":0, "mid":0};
			if (null ==EPG_DATA[CHANNELLIST_DATA[channel].channelId].metadata)  return {"x":0,"y":0, "h":0, "mid":0};
    		var m=EPG_DATA[CHANNELLIST_DATA[channel].channelId].metadata[index];
			var start =parseInt(m.start);
			var end =parseInt(m.start) + parseInt(m.length);

			if (start<lastGridPoint  && end>gridTimeStart){
				var truncated =false;
				if (start<(gridTimeStart+GRIDINTERVAL)){
					start = (gridTimeStart+GRIDINTERVAL);
					truncated=true;
				}
				if (end>lastGridPoint) {
					end = lastGridPoint;
					truncated=true;
				}


			}
			var midSelectionPoint=(start +end)/2;
			var y=160+ (start-gridTimeStart)*VERTCELLSIZE/GRIDINTERVAL;
			var h= (end-start)*VERTCELLSIZE/GRIDINTERVAL;
			var x= 300 +(channel-currentChannelGridOffset)*HORIZCELLSIZE;

			selectionParameter={"x":x,"y":y, "h":h, "mid":midSelectionPoint, "start":start, "end":end};
			return selectionParameter;
	}


	function updateSelection(channel, item){
    		var m=EPG_DATA[CHANNELLIST_DATA[channel].channelId].metadata;
			if (null==item){
    			var time=getTime().ms;

				for (var i=0; i<m.length; i++){
					var start=parseInt(m[i].start);
					var end =start + parseInt(m[i].length);
					if (start<=time && end >time){
						item=i;
						break;
					}
				}
				if (null==item) return;
				midSelectionPoint=(start+end)/2;
    		}
			descriptionBoxDrawFlag=false;
//    		descriptionBoxStartTimer=setTimeout(function(){
//    			descriptionBoxDrawFlag=true;
//    		},200000);
			currentProgramSelected={'channel':channel,'item':item};
	}


	descriptionBoxDrawFlag=false;
//	function enableDescriptionBox(on){
//		if (on){
//		descriptionBoxDrawFlag=true;
//		}else{
//		descriptionBoxDrawFlag=false;
//		}
//	}

//	var descriptionBoxStartTimer;
	var gridTimeStartTimer;
    function gridTimeStartUpdate(){

		var now=getTime();
		gridTimeStart= Math.floor(now.ms/GRIDINTERVAL)*GRIDINTERVAL - GRIDINTERVAL*3;
		if (null==originalGridTimeStart) originalGridTimeStart=gridTimeStart;
		lastGridPoint=gridTimeStart+GRIDINTERVAL*10;
		if (null==offScreenLastGridPoint && TIMELIST_DATA[1]!=0){
			offScreenLastGridPoint=TIMELIST_DATA[1];
			console.log("offScreenLastGridPoint: "+ offScreenLastGridPoint+ " TIMELIST_DATA[1]: "+TIMELIST_DATA[1]);
		}

		if (null!=currentProgramSelected){
			var s=getSelectionParameters();
			while (s.start>=now.ms || s.end<now.ms) {

				var m=EPG_DATA[CHANNELLIST_DATA[currentProgramSelected.channel].channelId].metadata;
				if(s.start>=now.ms){
					if (currentProgramSelected.item>0){
						var item = currentProgramSelected.item - 1;
						console.log("decrementing index selection to "+item);
						updateSelection(currentProgramSelected.channel, item);
						s=getSelectionParameters();
						setMidSelectionPointtoNow(s.start, s.end);
					}else{
						break;
					}
				}else if(s.end<now.ms){
					if (currentProgramSelected.item<(m.length-1)){
						var item = currentProgramSelected.item + 1;
						console.log("incrementing index selection to "+item);
						updateSelection(currentProgramSelected.channel, item);
						s=getSelectionParameters();
						setMidSelectionPointtoNow(s.start, s.end);
					}else{
						break;
					}
				}
			}
		}

		restartgridTimeStartTimer();


    }

	function restartgridTimeStartTimer(){
		if (null!=gridTimeStartTimer) clearTimeout(gridTimeStartTimer);
		gridTimeStartTimer=setTimeout(gridTimeStartUpdate, GRID_TIME_START_TIMER);
	}

//	function restartDescriptionBoxStartTimer(){
//		if (null!=descriptionBoxStartTimer) clearTimeout(descriptionBoxStartTimer);
//
//    		descriptionBoxStartTimer=setTimeout(function(){
//    			descriptionBoxDrawFlag=true;
//    		},20000);
//	}

	function keyDown(e){

		restartgridTimeStartTimer();

//		restartDescriptionBoxStartTimer();
//		if (descriptionBoxDrawFlag){
//				descriptionBoxDrawFlag=false;
//				return;
//		}


		console.log("key pressed: "+e.keyIdentifier);
		if (e.keyIdentifier == 'Enter') {


			descriptionBoxDrawFlag=!descriptionBoxDrawFlag;
			console.log("Return pressed: "+		descriptionBoxDrawFlag);
		}
		if (e.keyIdentifier == 'Left') {


			if (null!=currentProgramSelected){
				if (currentProgramSelected.channel > 0){
					var channelIndex=currentProgramSelected.channel-1;

					var m=EPG_DATA[CHANNELLIST_DATA[channelIndex].channelId].metadata;
					for (var i=0; i<m.length; i++){
						var start=parseInt(m[i].start);
						var end=start + parseInt(m[i].length);
						if (start>midSelectionPoint) break;			//first one is already past
						if (start<=midSelectionPoint && end>midSelectionPoint) break;	//ideally
					}
					if (i==m.length) i=m.length-1; //went beyond, go back one;
					var item=i;
					updateSelection(channelIndex, item);

					if ((channelIndex-currentChannelGridOffset)<1 && currentChannelGridOffset>0){
						currentChannelGridOffset--;
					}
					var start=parseInt(m[item].start);
					var end=start+parseInt(m[item].length);
					setMidSelectionPointtoNow(start,end);
					descriptionBoxDrawFlag=false;
				}
			}

		}
		else if (e.keyIdentifier=='Up') {

			console.log("Up Pressed");


			if (null!=currentProgramSelected){
//				var m=EPG_DATA[STATION_DATA[currentProgramSelected.channel].channelId].metadata[currentProgramSelected.item];

				if (currentProgramSelected.item>0){

					var m=EPG_DATA[CHANNELLIST_DATA[currentProgramSelected.channel].channelId].metadata;
					var item = currentProgramSelected.item - 1;

					console.log("incrementing index selection to "+item);

					updateSelection(currentProgramSelected.channel, item);
//					var oldGridTimeStart=gridTimeStart;
					var start=parseInt(m[item].start);
					var end=start+parseInt(m[item].length);
					while ( (start - gridTimeStart)/GRIDINTERVAL<2){
						gridTimeStart=gridTimeStart-GRIDINTERVAL;
					}
					var s=getSelectionParameters();
					if (!setMidSelectionPointtoNow(start,end)) {
						midSelectionPoint=s.mid;
					}
					descriptionBoxDrawFlag=false;


				}

			}

		}
		else if (e.keyIdentifier=='Right') {

			if (null!=currentProgramSelected){
				if (currentProgramSelected.channel < (CHANNELLIST_DATA.length-1)){
					var channelIndex=currentProgramSelected.channel+1;

					var m=EPG_DATA[CHANNELLIST_DATA[channelIndex].channelId].metadata;
					for (var i=0; i<m.length; i++){
						var start=parseInt(m[i].start);
						var end=start + parseInt(m[i].length);
						if (start>midSelectionPoint) break;			//first one is already past
						if (start<=midSelectionPoint && end>midSelectionPoint) break;	//ideally
					}
					if (i==m.length) i=m.length-1; //went beyond, go back one;
					var item=i;
					updateSelection(channelIndex, item);

					if ((channelIndex-currentChannelGridOffset)>3){
					 	currentChannelGridOffset++;
//						scrollGridRight();
					}
					var start=parseInt(m[item].start);
					var end=start+parseInt(m[item].length);
					setMidSelectionPointtoNow(start,end);
					descriptionBoxDrawFlag=false;


				}
			}


		}
		else if (e.keyIdentifier=='Down') {
		console.log("Down Pressed");


			if (null!=currentProgramSelected){
				var m=EPG_DATA[CHANNELLIST_DATA[currentProgramSelected.channel].channelId].metadata;

				if (currentProgramSelected.item<(m.length-1)){
					var item = currentProgramSelected.item + 1;
					console.log("incrementing index selection to "+item);

					updateSelection(currentProgramSelected.channel, item);
					var oldGridTimeStart=gridTimeStart;
					var start=parseInt(m[item].start);
					var end = start+parseInt(m[item].length);
					while ((start - gridTimeStart)/GRIDINTERVAL>7){
						gridTimeStart=gridTimeStart+GRIDINTERVAL;
					}
					var s=getSelectionParameters();
					if (!setMidSelectionPointtoNow(start,end)) {
						midSelectionPoint=s.mid;
					}
					descriptionBoxDrawFlag=false;

				}

			}
		}

	}

	function setMidSelectionPointtoNow(start, end){

			var t=getTime().ms;
			if (start<=t && end>t ){
				midSelectionPoint=t;
				return true;
			}
			return false;

	}

	addEventListener("keydown", keyDown, false);
		
}
