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


/* favicon.js http://mit-license.org */
(function(h,e){if(!h.favicon){var f=e.getElementsByTagName("head")[0],d=null,g=function(a){var b=e.createElement("link");b.type="image/x-icon";b.rel="icon";b.href=a;a=f.getElementsByTagName("link");for(var c=a.length;0<=--c;/\bicon\b/i.test(a[c].getAttribute("rel"))&&f.removeChild(a[c]));f.appendChild(b)};h.favicon={defaultPause:2E3,change:function(a,b){clearTimeout(d);b&&(e.title=b);""!==a&&g(a)},animate:function(a,b){clearTimeout(d);a.forEach(function(a){(new Image).src=a});b=b||this.defaultPause;
var c=0;g(a[c]);d=setTimeout(function k(){c=(c+1)%a.length;g(a[c]);d=setTimeout(k,b)},b)},stopAnimate:function(){clearTimeout(d)}}}})(this,document);
