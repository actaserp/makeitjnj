


let AjaxFunction = {
    PostAjaxRequest: function(url, async, contentType, processData, bodyData, successCallback, errorCallback){
        $.ajax({
            url: url,
            type: 'POST',
            contentType: contentType,
            async: async,   // 기본값: true
            data: bodyData,
            processData: processData,   //기본값: true
            headers: {
                'X-CSRF-Token': csrfToken
            },
            success: function(res){
                successCallback(res);
            },
            error: function(jqXHR, textStatus, errorThrown) {
                errorCallback(jqXHR, textStatus, errorThrown);
            }
        });
    }
};

// 로딩 바를 보여주는 함수
function showLoadingBar() {
    document.getElementById('loadingBar').style.display = 'block';
}

// 로딩 바를 숨기는 함수
function hideLoadingBar() {
    document.getElementById('loadingBar').style.display = 'none';
}









// 파일 리스트 UI 업데이트 함수
function updateFileListUI() {
    const $fileList = $('#filelist');

    uploadedFiles.forEach(file => {
        const fileSize = (file.size / 1024).toFixed(2) + ' KiB';
        const li = $('<li>').html(`
                    <p>${file.name} <span>(${fileSize})</span></p>
                    <a href="#" title="삭제" class="btn-file-delete">
                        <img src="/images/icon/ico-filedelete.svg" alt="삭제아이콘">
                    </a>
                `);
        $fileList.append(li);
    });
}


function updateTabLink(pkName){
    const pk = document.getElementById(pkName).value;
    const inputTabLink = document.getElementById('inputTabLink');

    if(pk){
        inputTabLink.textContent = '수정';
        inputTabLink.title = '수정'
    }else {
        inputTabLink.textContent = '등록';
        inputTabLink.title = '등록';
    }
}


function postFetchRequest(url, data, successCallback, errorCallback) {
    fetch(url, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            "X-CSRF-TOKEN": csrfToken
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            if (!response.ok) {
                return response.blob().then(blob => {
                    if (response.status === 404) {
                        throw new Error('첨부된 파일이 없습니다.');
                    } else {
                        throw new Error('Network Error.');
                    }
                });
            }
            return response.blob();
        })
        .then(blob => {
            successCallback(blob);
        })
        .catch(error => {
            errorCallback(error);
        })
        .finally(() => {
            hideLoadingBar();
        })
}

function ElementBinding(element, paramvalue){
    if(paramvalue !== null){
        document.getElementById(element).value = paramvalue;
    }

}

function checkEmptySelectedItem(SelectItem, pk) {
    const hasEmpty = SelectItem.some(r => {
        if (r._data === "empty") {

            return true;
        }
        if (r._data[pk] === undefined) {

            return true;
        }
        return false;
    });

    return !hasEmpty;
}

function SelectItemPush(array, propertyPath){

    console.log('trigger: ');
    console.log('array: ', array);
    console.log('propertyPath: ', propertyPath);


    return array.map(item => {
        // Split the property path by dot (.) to support nested properties
        const properties = propertyPath.split('.');
        let value = item;

        // Iterate over the properties to access the nested value
        for (let i = 0; i < properties.length; i++) {
            if (value == null) {
                return undefined;
            }
            value = value[properties[i]];
        }
        return value;
    });
}
//셀렉트 박스 동적 바인딩
function initializeSelect({
                              url,               // API 엔드포인트 URL
                              params = {},       // 요청 매개변수 (기본값은 빈 객체)
                              elementId,         // 셀렉트 요소의 ID
                              defaultOption = "선택하세요",  // 기본 옵션 텍스트
                              valueField = "code",    // 데이터의 값 필드 이름
                              textField = "value"     // 데이터의 표시 필드 이름
                          }) {
    $.get(url, params, function(data){
        console.log('확인: ', data);
        let selectElement = $(`#${elementId}`);
        selectElement.empty();
        selectElement.append(`<option value="">${defaultOption}</option>`);
        data.forEach(function(item) {
            selectElement.append(`<option value="${item[valueField]}">${item[textField]}</option>`);
        });
    });
}

//공통코드 사용
function initializeSelect3({
                               url,
                               params = {},
                               elementId,
                               defaultOption = "선택하세요",  // 기본 옵션 유지
                               includeAllOption = false,  // "전체" 옵션 추가 여부
                               valueField = "code",
                               textField = "value",
                               defaultValue = null  // 기본 선택값 추가
                           }) {
    $.get(url, params, function(response) {
        const data = response.data || [];
        let selectElement = $(`#${elementId}`);
        if (!selectElement.length) {
            return;
        }

        selectElement.empty();

        // "전체" 옵션이 없을 때만 "선택하세요" 추가
        if (!includeAllOption) {
            selectElement.append(`<option value="">${defaultOption}</option>`);
        } else {
            selectElement.append(`<option value="all">전체</option>`);
        }

        // API 데이터 추가
        if (Array.isArray(data)) {
            data.forEach(function(item) {
                selectElement.append(`<option value="${item[valueField]}">${item[textField]}</option>`);
            });
        }
        // defaultValue가 설정된 경우에만 적용
        if (defaultValue !== null && defaultValue !== undefined) {
            selectElement.val(defaultValue);
        }
    }).fail(function(xhr, status, error) {
        Alert.alert("데이터를 불러오는 중 문제가 발생했습니다. 관리자에게 문의하세요.", '');
    });
}



//이메일 정규식
function emailValidate(emailVal){
    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    return emailPattern.test(emailVal);
}

//같은 name값의 select요소 value 추출
function extractSelectValues(selectName) {
    let inputs = document.querySelectorAll(`select[name="${selectName}"]`);
    return Array.from(inputs).map(input => input.value).join(',');
}

//같은 name값의 select요소 텍스트 추출
function extractSelectTexts(selectName) {
    let inputs = document.querySelectorAll(`select[name="${selectName}"]`);
    return Array.from(inputs).map(input => input.options[input.selectedIndex].text).join(',');
}


// 유효성 검사
function validateFields() {
    for (let field of requiredFields) {
        if (!validateFieldById(field.id, field.name)) {
            return false;
        }
    }

    for (let field of nameBasedFields) {
        if (!validateFieldsByName(field.name, field.displayName)) {
            return false;
        }
    }

    return true;  // 모든 유효성 검사를 통과했을 경우
}

// ID로 유효성 검사 함수
function validateFieldById(id, fieldName) {
    const value = document.getElementById(id)?.value.trim();

    if (!value) {
        Alert.alert('', `${fieldName}이(가) 입력되지 않았습니다.`);
        return false;
    }

    return true;
}

// name 속성으로 유효성 검사 함수
function validateFieldsByName(name, fieldName) {
    const elements = document.getElementsByName(name);

    if (elements.length === 0) {
        console.error(`이름이 ${name}인 요소를 찾을 수 없습니다.`);
        return false;
    }

    for (let element of elements) {
        const value = element.value.trim();
        if (!value) {
            Alert.alert('', `${fieldName}이(가) 입력되지 않았습니다.`);
            return false;
        }
    }

    return true;
}


//############################날짜함수

//당일
function getDateYYYYMMDD(){
    const date = new Date();
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2,'0');

    return `${year}-${month}-${day}`
}

//당월 마지막일
function getLastDayOfCurrentMonth() {
    const date = new Date();
    const year = date.getFullYear();
    const month = date.getMonth() + 1; // 현재 달 (0-11이므로 +1)

    // 다음 달의 첫째 날을 계산한 후, 하루를 빼서 당월 마지막 날을 구함
    const lastDayDate = new Date(year, month, 0);
    const day = String(lastDayDate.getDate()).padStart(2, '0');
    const lastMonth = String(lastDayDate.getMonth() + 1).padStart(2, '0');

    return `${year}-${lastMonth}-${day}`;
}

//특정날짜의 월 구하기
function getMonth(dateString){
    const date = new Date(dateString);
    const month = date.getMonth() + 1;
    return month;
}

function fetchAndPopulateData(userid, dataListId, active) {
    $.ajax({
        url: '/api/system/auth_list/tb_rp945List',
        type: 'GET',
        data: {
            'userid': userid
        },
        success: function(data) {
            console.log(data);

            const listLen = data.data.length;
            const parent = $('#' + dataListId);

            // 기존 리스트를 비우고 새 데이터를 추가
            parent.empty();

            for (let i = 0; i < listLen; i++) {
                const newLi = $('<li>');

                const firstInput = $('<input>', {
                    type: 'text',
                    id: 'spworknm' + (i + 1),
                    name: 'spworknm' + (i + 1),
                    class: 'w150',
                    style: 'margin: 3px 5px',
                    readOnly: active,
                    value: data.data[i].spworknm
                });

                const secondInput = $('<input>', {
                    type: 'text',
                    id: 'spcompnm' + (i + 1),
                    name: 'spcompnm' + (i + 1),
                    class: 'w150',
                    style: 'margin: 3px 5px',
                    readOnly: active,
                    value: data.data[i].spcompnm
                });

                const thirdInput = $('<input>', {
                    type: 'text',
                    id: 'spplannm' + (i + 1),
                    name: 'spplannm' + (i + 1),
                    class: 'w150',
                    style: 'margin: 3px 5px',
                    readOnly: active,
                    value: data.data[i].spplannm
                });

                newLi.append(firstInput).append(secondInput).append(thirdInput);
                parent.append(newLi);
            }
        },
        error: function(xhr, status, error) {
            console.log('error');
        }
    });
}



//서버에서 데이터 가져오기  (type이 rmate인것이랑 wijmo랑 succ함수 다름.)
function DataLoad(url, dataSet){

    let data2;

    $.ajax({
        url: url,
        type: 'GET',
        data: dataSet,
        async: false,
        success: function(data){


                data2 = data;

        }
    })
    return data2;
}

$(document).ready(function (e) {
    //점검결과 클릭시 텍스트 순환
    $('.checkbox-cell').click(function() {
        const span = $(this).find('span');
        if (span.text() === 'X') {
            span.text('O');
        } else if (span.text() === 'O') {
            span.text('X');
        }
    });

    /**
     * 탭 기능을 초기화하는 함수
     * @param {string} containerSelector - 탭 기능을 적용할 컨테이너의 선택자
     */
    function initializeTabs(containerSelector) {
        $(containerSelector).each(function() {
            var $container = $(this);

            // 탭 클릭 이벤트 처리
            $container.find(".tab-links a").click(function(event) {
                event.preventDefault();

                // 클릭된 탭 링크의 href 속성 값을 가져옴
                var tabId = $(this).attr("href");

                // 해당 탭을 보여주고 활성화
                $container.find(".tab-item").hide();
                $container.find(tabId).show();

                // 현재 활성화된 탭을 나타내기 위해 클래스 추가/제거
                $container.find(".tab-links li").removeClass("active");
                $(this).parent().addClass("active");
            });

            // 초기에 첫 번째 탭을 활성화
            $container.find(".tab-item:first").show();
            $container.find(".tab-links li:first").addClass("active");
        });

    }

    function initializeTabs_sub(containerSelector) {
        $(containerSelector).each(function() {
            var $container = $(this);

            // 탭 클릭 이벤트 처리
            $container.find(".tab-links-sub a").click(function(event) {
                event.preventDefault();

                // 클릭된 탭 링크의 href 속성 값을 가져옴
                var tabId = $(this).attr("href");

                // 해당 탭을 보여주고 활성화
                $container.find(".tab-item-sub").hide();
                $container.find(tabId).show();

                // 현재 활성화된 탭을 나타내기 위해 클래스 추가/제거
                $container.find(".tab-links-sub li").removeClass("active");
                $(this).parent().addClass("active");
            });

            // 초기에 첫 번째 탭을 활성화
            $container.find(".tab-item-sub:first").show();
            $container.find(".tab-links-sub li:first").addClass("active");
        });

    }


    // 페이지 내의 다양한 탭 컨테이너를 초기화
    initializeTabs(".tab-section"); // 첫 번째 페이지의 탭
    initializeTabs(".another-tab-section"); // 두 번째 페이지의 탭
    initializeTabs_sub(".tab-section"); // 첫 번째 페이지의 탭
    initializeTabs_sub(".another-tab-section"); // 두 번째 페이지의 탭

})

function toggleSearchWrap(){
    let searchWrap = document.getElementById('searchWrap');
    let toggleButton = document.getElementById('toggleButton').querySelector('img');


    if(searchWrap.style.display === 'none' || searchWrap.style.display === ''){
        searchWrap.style.display = 'flex';
        toggleButton.src = '/images/icon/ico-up.png';

    } else {
        searchWrap.style.display = 'none';
        toggleButton.src = '/images/icon/ico-down.png';
    }
}

//날짜컬럼 짝대기 붙혀서 ex: 2024-01-01
function formatDate(dateStr){
    if(dateStr.length === 8){
        return `${dateStr.slice(0,4)}-${dateStr.slice(4,6)}-${dateStr.slice(6,8)}`;

    }
    return dateStr;
}

function FormDataInsert(formData) {
    // 기본 필드들을 추가
    const defaultFields = ['spworkcd', 'spworknm', 'spcompcd', 'spcompnm', 'spplancd', 'spplannm'];

    defaultFields.forEach(field => {
        formData.append(field, $(`#${field}`).val());
    });

    return formData; // formData에 기본 필드를 추가한 후 반환
}









