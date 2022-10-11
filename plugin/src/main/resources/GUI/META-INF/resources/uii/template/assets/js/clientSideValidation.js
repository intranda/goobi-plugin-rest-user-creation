
const clientSideValidation = (function (){

  // DEBUG
  const debug = false;
  if (debug) console.log('## Client Side Validation ##');

  const compareInputValues = {
    compare({inputEl, inputConfirmEl, msgSuccess, msgError}) {  

      const infoEl = document.createElement('div');
      const infoTextSucc = msgSuccess; 
      const infoTextErr = msgError;

      const isEmpty = (inputEl.value === '' || inputConfirmEl.value === '');
      const isEqual = inputEl.value === inputConfirmEl.value;

      inputEl.parentElement.querySelector('.text-danger')?.remove();
      inputEl.parentElement.querySelector('.text-success')?.remove();


      if (isEmpty) {
          inputEl.style.borderColor = 'var(--border-color)';
          inputConfirmEl.style.borderColor = 'var(--border-color)';
          return
      }

      if(isEqual) {
          inputEl.style.borderColor = 'var(--clr-success)';
          inputConfirmEl.style.borderColor = 'var(--clr-success)';
          infoEl.classList.add('text-success', 'validation-message--clientside');
          infoEl.innerText = infoTextSucc;
          inputEl.parentElement.appendChild(infoEl);
          return
      }

      inputEl.style.borderColor = 'var(--clr-danger)';
      inputConfirmEl.style.borderColor = 'var(--clr-danger)';
      infoEl.classList.add('text-danger', 'validation-message--clientside');
      infoEl.innerText = infoTextErr;
      inputEl.parentElement.appendChild(infoEl);

    },

    init(params) {
      const input = document.querySelector('#password');
      const inputConfirm = document.querySelector('#password2');

      params.inputConfirmEl = inputConfirm;
      params.inputEl = input;

      if (debug) console.log(params)

      input.addEventListener('keyup', () => compareInputValues.compare(params));
      inputConfirm.addEventListener('keyup', () => compareInputValues.compare(params));
    }
  }

  return {
    compareInputValues
  }

})()

