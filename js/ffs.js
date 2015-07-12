$(window).load(function() {
    $('.placeholder').hide();
    $('.preload').attr('src', function(i,a){
        $(this).attr('src','').removeClass('preload').attr('src',a);
    });
});
