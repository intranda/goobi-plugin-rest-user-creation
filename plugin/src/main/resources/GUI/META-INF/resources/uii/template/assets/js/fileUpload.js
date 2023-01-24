
var fileUpload = (function fileUpload(){

  function shortenFileName(fileName) {

    // Check length of the file name
    if (fileName.length > 80) {
      // shorten if too long, and
      // inject
      const part1 = fileName.slice(0, 32);
      const part2 = fileName.slice(-32);
      return part1 + '...' + part2

    } else {
      return fileName
    }
  }

  function updateFE(e) {
    const [file] = e.target.files;
    const iconFirst = document.querySelector('.form__icons:first-of-type')
    const iconSecond = document.querySelector('.form__icons:nth-of-type(2)')
    const fileName = document.querySelector('.form__file-name');
    const formMsg = document.querySelector('.form__msg');
    const formMsgOr = document.querySelector('.form__msg-or');
    const fakeBtnSend = document.querySelector('.fake-btn--send');
    const btnUrlUpload = document.querySelector('.btn--url-upload');


    // Check length of the file name
    if (file.name.length > 80) {
      // shorten if too long, and
      // inject
      const part1 = file.name.slice(0, 32);
      const part2 = file.name.slice(-32);
      fileName.innerHTML = part1 + '...' + part2

    } else {
      // inject filname as is
      fileName.innerHTML = file.name
    }

    // Hide icon, upload message + url upload btn
    iconFirst.classList.add('d-none');
    formMsg.classList.add('d-none');
    formMsgOr.classList.add('d-none');
    btnUrlUpload.classList.add('d-none');


    // Show file names + upload icon
    fileName.classList.remove('d-none');
    iconSecond.classList.remove('d-none');

    // Enable fake send btn
    // fakeBtnSend.removeAttribute('disabled');
    fakeBtnSend.classList.remove('d-none');
  }

  function storeFileName(e) {
      console.log(1)
    const [file] = e.target.files;
    const fileName= shortenFileName(file.name);  
    sessionStorage.setItem('zlb-file-name.temp', fileName);

  }

  function showInfo() {
    const uploadForm = document.querySelector('#uploadForm')
    const formMsgDiv = document.querySelector('.form__msg');
    const formMsgText = document.querySelector('.form__msg__text');
    const formMsgOr = document.querySelector('.form__msg-or');
    const formActions = document.querySelector('.form__actions');
    const globalMsg = document.querySelector('#globalMessage');
    const fileName = sessionStorage.getItem('zlb-file-name.temp'); 

      console.log(fileName)

    if(fileName) {
      // Insert fileName + add styling
      formMsgText.innerText = fileName;
      formMsgText.style.color = "var(--clr-primary-400)"
      formMsgText.style.display = "block"
      formMsgText.style.marginBottom = "8px"

      formMsgOr.style.marginTop = '8px';

      // Hide controls => prevent another upload
      /*
      formMsgOr.style.display = 'none';
      formActions.style.display = 'none';
      uploadForm.style.pointerEvents = 'none';
      */

      const btnNext = document.querySelector('.btn-success')
      btnNext?.classList.add('glowing')
    }

    // Append alert
    formMsgDiv.appendChild(globalMsg);
    formMsgDiv.style.maxWidth = '720px';

    // Clean up
    sessionStorage.removeItem('zlb-file-name.temp')
  }

  function init({DSForm, DSSubmit}) {

    const globalMsg = document.querySelector('#globalMessage .messages');
    if (globalMsg) showInfo();

    const form = document.querySelector(DSForm);
    const submit = document.querySelector(DSSubmit);
    const fakeBtnBrowse = document.querySelector('.fake-btn--browse');
    const fakeBtnSend= document.querySelector('.fake-btn--send');

    if(!form || !submit) return;

    // Listen for file submit => update Frontend 
    // form.addEventListener('change', updateFE);
    //
    // Upload pdf immediately after choosing a file 
    // => eliminates a step where the user has to press an 'upload' button (updateFE())
    form.addEventListener('change', (e) => {storeFileName(e); submit.click()}); 

    // Listen for click on Browse btn => click on the form to open file browser
    fakeBtnBrowse.addEventListener('click', function() {
      form.click();;
    })

    // Listen for click on Send btn => click on hidden submit btn 
    fakeBtnSend.addEventListener('click', function() {
      submit.click();;
    })


  }

  return {
    init
  }

})()
