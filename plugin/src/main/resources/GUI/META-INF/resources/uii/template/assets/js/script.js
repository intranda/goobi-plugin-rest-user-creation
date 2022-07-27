$("document").ready(function () {

  // Scripts for modals can be found in `resources/composites/modals.xhtml`
  // => "Inline Help" + page help

  // Activate Tooltips
  var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"], [data-toggle="tooltip"]'))
  var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  });


  /*
  // show inline help
  const form = document.querySelector('form');
  form.addEventListener('click', handleClick);

  function handleClick(e) {
      if(e.target.classList.contains('form-icons__button--help')) {
          const formElement = e.target.parentElement.parentElement
          const helpText = formElement.querySelector('.help-text');
          helpText.classList.toggle('vis-hidden');
      }
  }
  */


});
