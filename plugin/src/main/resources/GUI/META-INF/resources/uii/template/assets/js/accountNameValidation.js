
const accountNameValidation = (function() {


    const validateInputValue = {
        validate({ inputEl, msgSuccess, msgError }) {


            const infoEl = document.createElement('div');
            const infoTextSucc = msgSuccess;
            const infoTextErr = msgError;

            const isEmpty = (inputEl.value === '');
            const isValid = /^[a-zöüä0-9_]+$/.test(inputEl.value);

            inputEl.parentElement.querySelector('.text-danger')?.remove();
            inputEl.parentElement.querySelector('.text-success')?.remove();


            if (isEmpty) {
                inputEl.style.borderColor = 'var(--border-color)';
                return
            }

            if (isValid) {
                inputEl.style.borderColor = 'var(--clr-success)';
                infoEl.classList.add('text-success', 'validation-message--clientside');
                infoEl.innerText = infoTextSucc;
                inputEl.parentElement.appendChild(infoEl);
                return
            }

            inputEl.style.borderColor = 'var(--clr-danger)';
            infoEl.classList.add('text-danger', 'validation-message--clientside');
            infoEl.innerText = infoTextErr;
            inputEl.parentElement.appendChild(infoEl);

        },

        init(params) {
            const input = document.querySelector('#accountName');

            params.inputEl = input;


            input.addEventListener('keyup', () => validateInputValue.validate(params));
        }
    }

    return {
        validateInputValue
    }

})()

