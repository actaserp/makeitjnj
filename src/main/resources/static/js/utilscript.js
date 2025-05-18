function submitTextarea(event, func){
    let key = event.key || event.keyCode;

    if(key === 'Enter' || key == 13){
        func();
    }
}

//오늘낧짜
function getNowDate(){
    const today = new Date();

    const formattedDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`
    }
    return formattedDate(today);
}
// 7일전 날짜
function getNowDateMinus7(){

    const day = new Date();

    const sevenDaysAgo = new Date();

    sevenDaysAgo.setDate(day.getDate() - 7);

    const formattedDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };
    return formattedDate(sevenDaysAgo);
}

//현재 시간, HH:mm
function getNowTime(){
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`
}

function getFirstDayOfThreeMonthsAgo(){
    const today = new Date();

    const threeMonthsAgo = new Date(today.getFullYear(), today.getMonth() - 3, 1);

    const formattedDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };
    return formattedDate(threeMonthsAgo);
}

// 3달이상이면 false
function calculateDay(fd, td){



    const frdate = new Date(fd);
    const todate = new Date(td);

    const diffInMs = Math.abs(frdate - todate);
    const diffDays = diffInMs / (1000 * 60 * 60 * 24);

    return diffDays <= 90;
}

// 빈 값 항목 체크해줌
function validationCheck(FormData, checklist){

    for(let key in checklist){
        const value = FormData[key];

        if(value === null || value === ''){
            const text = checklist[key];

            Alert.alert('', `${text} 항목이 빈 값입니다.`);
            return false;
        }
    }
    return true
}

// api 호출후 응답메세지 반환
function ApiSuccessMessage(res){
    if(res === null || res === undefined){
        Alert.alert('', '에러가 발생하였습니다.');
    }
    Alert.alert('', res.message);
}

//셀렉트 박스 바인딩 해줌
function selectBoxBinding(id, arr){

    const selectedBox = document.getElementById(id);
    selectedBox.innerHTML = '';

    arr.forEach(item => {
        const option = document.createElement('option');
        option.value = item.value;
        option.textContent = item.text;
        selectedBox.appendChild(option);
    })
}

//입출금 구분에 따라 거래구분을 필터링 후 콤보박스에 바인딩
//inout : 입금(0)이냐 출금(1)이냐
//arr : 서버에서 조회환 항목들
//id : 바인딩하려는 콤보박스 아이디
function filterAndBindTradeTypes(inout, arr, id){
    const filterItems = arr.filter(item => item.ioflag === inout);

    const selectedBox = document.getElementById(id);
    selectedBox.innerHTML = '';

    filterItems.forEach(item => {
        const option = document.createElement('option');
        option.value = item.value;
        option.textContent = item.text;
        selectedBox.appendChild(option);
    })
}

function getSpjangcdFromSession(){
    return sessionStorage.getItem('spjangcd');
}

//거래처 팝업 오픈해주는 함수
function companyPopupOpen(intputId, hiddenid){

    let poppage = new PopCompComponent();
    let $poppage = $(poppage);
    let searchcallback = function (items) {
        // $content.find('#cboCompany').val(items.id);
        // $content.find('#CompanyName').val(items.compname);

        document.getElementById(intputId).value = items.compname;
        document.getElementById(hiddenid).value = items.id;
    };

    poppage.show(searchcallback);
}

//숫자 금액으로 변환 , 숫자 아니면 0으로 리턴
function formatMoney(val){
    const num = Number(val);
    if(isNaN(num)) return '0';
    return num.toLocaleString('en-US')
}

