var gs;
var TWELVEHOUR=true;
function start(){

    var v=document.getElementById("video");
    v.src="http://192.168.1.8:4004/ODIDMediaServer/i/ZTViZjU1ZWNhYWMyMDYwOWVkNTNiZGFmYmZmZjU4NGI=/res/B-MP2PS_N-8.0.mpg"
    v.onloadstart=function(){
        console.log("Video loaded");
        this.play();
    }
    gs=new GuideController();

}

function getTime(ms){
		if (null!=ms)
			var today=new Date(ms);
		else
			var today=new Date();
		var jsonTime;
		var todayUTCstring = today.toUTCString();
		var displayTodayArray=todayUTCstring.split(' ');
		var displayDate=displayTodayArray[0]+' '+displayTodayArray[1]+' '+displayTodayArray[2]+' '+displayTodayArray[3];
		var ampm="";
		var hours=today.getHours();
		if (hours>12 && TWELVEHOUR)	{
			hours-=12;
			ampm="p"
		}else if (hours>11 && TWELVEHOUR){
			ampm="p"
		}else if (hours==0 && TWELVEHOUR){
			hours=12;
			ampm="a";
		}else if(TWELVEHOUR){
			ampm="a";
		}
		var displayTime=hours+":";
		var displayMins=today.getMinutes()<10?("0"+today.getMinutes()):(""+today.getMinutes());
		var displayTime=displayTime + displayMins + ampm;
		jsonTime={'ms':today.getTime(),"date":displayDate, "time": displayTime};
		return jsonTime;

}

function test(){

	var test=new TEST();
	test.initialize();


}