
Drupal.behaviors.multiselectSelector = function() {
  // Automatically selects the right radio button in a multiselect control.
  $('.multiselect select:not(.multiselectSelector-processed)')
    .addClass('multiselectSelector-processed').change(function() {
      $('.multiselect input[value="'+ this.id.substr(5) +'"]')
        .attr('checked', true);
  });
};

$(window).on('load',function(){
  $(".btn1").click(function(){
    $("p").slideToggle();
  });
  
  $('#edit-submit').submit(function(e){
    alert("Submitted");
	return false;
  });

});



