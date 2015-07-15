$(window).load(function() {
    favParts = []
    for (var i = 0; i < 22; i++)
      favParts.push("images/favicon/"+i+".gif");
    favicon.animate(favParts, 150);
    $('.placeholder').hide();
    $('.preload').attr('src', function(i,a){
        $(this).attr('src','').removeClass('preload').attr('src',a);
    });
});


var song = new Audio("spin.ogg");
song.autoplay = true;
song.loop = true;
song.volume = 0;
song.addEventListener('timeupdate', function(){
var buffer = .3
if(this.currentTime > this.duration - buffer){
    this.currentTime = 0
    this.play()
}}, false);
var vol = 0;
var onTop = false;

setInterval(function () {
  if(onTop) {
    vol = Math.min(vol + 0.001, 0.1);
  }
  else {
    vol = Math.max(vol - 0.001, 0);
  }
  song.volume = vol;
}, 10);

function fadeIn() {
  onTop = true;
}

function fadeOut() {
  onTop = false;
}

/* favicon.js http://mit-license.org */
(function(h,e){if(!h.favicon){var f=e.getElementsByTagName("head")[0],d=null,g=function(a){var b=e.createElement("link");b.type="image/x-icon";b.rel="icon";b.href=a;a=f.getElementsByTagName("link");for(var c=a.length;0<=--c;/\bicon\b/i.test(a[c].getAttribute("rel"))&&f.removeChild(a[c]));f.appendChild(b)};h.favicon={defaultPause:2E3,change:function(a,b){clearTimeout(d);b&&(e.title=b);""!==a&&g(a)},animate:function(a,b){clearTimeout(d);a.forEach(function(a){(new Image).src=a});b=b||this.defaultPause;
var c=0;g(a[c]);d=setTimeout(function k(){c=(c+1)%a.length;g(a[c]);d=setTimeout(k,b)},b)},stopAnimate:function(){clearTimeout(d)}}}})(this,document);

/* Google Analytics */
(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
})(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', 'UA-65076556-1', 'auto');
ga('send', 'pageview');
