var TEST=function(){

   	var ctx;
   	var background;
   	var width, length, mouse;

   this.initialize=function (){
        canvas = document.getElementById('canvas');
        width = canvas.width;
        height = canvas.height;
        ctx = canvas.getContext('2d');
        ctx.font = "30px arial";
        background = new Image();
        background.src = 'img/background.png';

        var t= "jhkdfjkd sjkdbfkjasdfb jskdfnkjas jksdnfksd dsfkdf sdjfkhsjdf sdjfhkjsdfn sdjfksdfn sjkdfbhjsdf";
        drawTextInBox(100,100,300,t,1,2,4);
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
					console.log("words: " +words[j]+"  length: "+wordslength[j]);

				}
				phraseline=new Array(multiline);
				phraselength=new Array(multiline);
				for (var j=0; j<phraseline.length; j++){
					phraseline[j]='';
					phraselength[j]=0;
				}
				var phraseIndex=0;
				var nextWord=1;
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
				var middle=y+j*fontheights[fontindex]*1.5;
				console.log("left: "+left+"  middle: "+middle+"  width:  "+actualWidth);
				ctx.fillText(phraseline[j],left, middle, w);
			}
		}
		ctx.restore();
	}


}