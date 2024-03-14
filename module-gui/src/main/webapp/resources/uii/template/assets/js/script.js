// Scripts for modals can be found in `resources/composites/modals.xhtml`
// => "Inline Help" + page help

// Activate Bootstrap 5 Tooltips
function tooltipsInit() {

  var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"], [data-toggle="tooltip"]'))
  var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  });

}

$("document").ready(function () {
  tooltipsInit();

});
