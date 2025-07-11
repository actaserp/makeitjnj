// 선택된 sandanData를 sessionStorage에 저장
function saveSelectedSandanData() {
    const spworkcd = document.getElementById('spworkcd').value;
    const spcompcd = document.getElementById('spcompcd').value;
    const spplancd = document.getElementById('spplancd').value;

    sessionStorage.setItem('selectedSpworkcd', spworkcd);
    sessionStorage.setItem('selectedSpcompcd', spcompcd);
    sessionStorage.setItem('selectedSpplancd', spplancd);
}

// sessionStorage에서 선택된 sandanData 불러오기
function loadSelectedSandanData() {
    const spworkcd = sessionStorage.getItem('selectedSpworkcd');
    const spcompcd = sessionStorage.getItem('selectedSpcompcd');
    const spplancd = sessionStorage.getItem('selectedSpplancd');

    if (spworkcd) {
        document.getElementById('spworkcd').value = spworkcd;
        updateCompcdOptions();
    }
    if (spcompcd) {
        document.getElementById('spcompcd').value = spcompcd;
        updatePlancdOptions();
    }
    if (spplancd) {
        document.getElementById('spplancd').value = spplancd;
        updatePlannm();
    }
}

// 산단 연동
function updateCompcdOptions() {
    var spworkcd = document.getElementById('spworkcd').value;
    var spworknm = sandanData.find(item => item.spworkcd === parseInt(spworkcd)).spworknm;
    document.getElementById('spworknm').value = spworknm; // spworknm 업데이트

    var spcompcdSelect = document.getElementById('spcompcd');
    spcompcdSelect.innerHTML = ''; // 기존 옵션 제거

    var filteredCompcdItems = sandanData
        .filter(function (item) {
            return item.spworkcd === parseInt(spworkcd);
        })
        .reduce(function (accumulator, current) {
            // spcompcd 중복 제거
            if (!accumulator.some(item => item.spcompcd === current.spcompcd)) {
                accumulator.push(current);
            }
            return accumulator;
        }, []);

    filteredCompcdItems.forEach(function (item) {
        var option = document.createElement('option');
        option.value = item.spcompcd;
        option.textContent = item.spcompnm;
        spcompcdSelect.appendChild(option);
    });

    if (filteredCompcdItems.length > 0) {
        spcompcdSelect.selectedIndex = 0;
        updatePlancdOptions(); // 첫 번째 산단 선택 시 시설 옵션 업데이트
    }

}

// 산단 연동
function updatePlancdOptions() {
    var spcompcd = document.getElementById('spcompcd').value;
    var spcompnm = sandanData.find(item => item.spcompcd === parseInt(spcompcd)).spcompnm;
    document.getElementById('spcompnm').value = spcompnm; // spcompnm 업데이트

    var spplancdSelect = document.getElementById('spplancd');
    spplancdSelect.innerHTML = ''; // 기존 옵션 제거

    var filteredPlancdItems = sandanData.filter(function (item) {
        return item.spcompcd === parseInt(spcompcd);
    });

    filteredPlancdItems.forEach(function (item) {
        var option = document.createElement('option');
        option.value = item.spplancd;
        option.textContent = item.spplannm;
        spplancdSelect.appendChild(option);
    });

    if (filteredPlancdItems.length > 0) {
        spplancdSelect.selectedIndex = 0;
        updatePlannm(); // spplancd 변경 시 spplannm 업데이트
    }
}

function updatePlannm() {
    const spplancd = document.getElementById('spplancd').value;
    const spplannm = sandanData.find(item => item.spplancd === parseInt(spplancd)).spplannm;
    document.getElementById('spplannm').value = spplannm; // spplannm 업데이트
}

// 산단 연동
function initializeSelections() {
    var spworkcdSelect = document.getElementById('spworkcd');
    spworkcdSelect.innerHTML = ''; // 기존 옵션 제거

    var uniqueWorkcdItems = [...new Set(sandanData.map(item => item.spworkcd))];

    uniqueWorkcdItems.forEach(function (spworkcd) {
        var item = sandanData.find(function (item) {
            return item.spworkcd === parseInt(spworkcd);
        });
        var option = document.createElement('option');
        option.value = item.spworkcd;
        option.textContent = item.spworknm;
        spworkcdSelect.appendChild(option);
    });

    loadSelectedSandanData();  // sessionStorage에서 이전 선택 사항 불러오기

    if (uniqueWorkcdItems.length > 0 && !sessionStorage.getItem('selectedSpworkcd')) {
        spworkcdSelect.selectedIndex = 0;  // 선택된 값이 없을 때만 초기화
        updateCompcdOptions(); // 첫 번째 지역 선택 시 산단 옵션 업데이트
    }
}

// 현재 년도를 가져옴
function getCurrentYear() {
    return new Date().getFullYear();
}

// 날짜 초기화 함수
function setDefaultDates(startDateId, endDateId) {

    // 순회점검 현황에서 더블클릭해서 새창으로 띄운다음 그 날짜 설정하기 위해서
    let checkdt = $('#checkdtParam').val().trim();

    if(checkdt !== null && checkdt !== ''){
        let searchdate = checkdt.slice(0, 4) + "-" + checkdt.slice(4, 6) + "-" + checkdt.slice(6, 8);
        document.getElementById("startDate").value = searchdate;
        document.getElementById("endDate").value = searchdate;

    }else{
        const currentYear = getCurrentYear();
        document.getElementById(startDateId).value = `${currentYear}-01-01`;
        document.getElementById(endDateId).value = `${currentYear}-12-31`;
    }

}

// 파일 다운로드
function downloadFiles(downloadList, url) {
    let xhr = new XMLHttpRequest();
    xhr.open('POST', url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.setRequestHeader('X-CSRF-Token', document.querySelector('[name=_csrf]').value);
    xhr.responseType = 'blob';

    xhr.onload = function () {
        if (xhr.status === 200) {
            let blob = xhr.response;
            let link = document.createElement('a');
            let downloadUrl = window.URL.createObjectURL(blob);
            let contentDisposition = xhr.getResponseHeader('Content-Disposition');
            let fileName = 'downloadedFile';

            if (contentDisposition) {
                let fileNameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                let matches = fileNameRegex.exec(contentDisposition);
                if (matches != null && matches[1]) {
                    fileName = decodeURIComponent(matches[1].replace(/['"]/g, ''));
                }

                // filename* 속성에서 파일 이름 추출
                let fileNameStarRegex = /filename\*\=([^;]*)/;
                let matchesStar = fileNameStarRegex.exec(contentDisposition);
                if (matchesStar != null && matchesStar[1]) {
                    fileName = decodeURIComponent(matchesStar[1].split("''")[1].replace(/['"]/g, ''));
                }
            }

            link.href = downloadUrl;
            link.download = fileName;
            link.style.display = 'none';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(downloadUrl);
        } else if (xhr.status === 404) {
            Alert.alert('', '파일이 존재하지 않습니다.');
        } else {
            Alert.alert('', '파일 다운로드 중 오류가 발생했습니다.');
        }
    };

    xhr.onerror = function () {
        console.error('Network error');
    };

    xhr.send(JSON.stringify(downloadList));
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

// 파일 리스트 UI 업데이트 함수
function updateFileListUI2() {
    const $fileList = $('#filelist2');

    uploadedFiles2.forEach(file => {
        const fileSize = (file.size / 1024).toFixed(2) + ' KiB';
        const li = $('<li>').html(`
                    <p>${file.name} <span>(${fileSize})</span></p>
                    <a href="#" title="삭제" class="btn-file-delete2">
                        <img src="/images/icon/ico-filedelete.svg" alt="삭제아이콘">
                    </a>
                `);
        $fileList.append(li);
    });
}

// 날짜 형식을 변환하는 함수
function formatDate(dateString) {
    if (dateString && dateString.length === 8) {
        return dateString.substring(0, 4) + '-' + dateString.substring(4, 6) + '-' + dateString.substring(6, 8);
    }
    return dateString;
}




// 파일 개수 업데이트 함수
function updateFileCount() {
    const fileCount = uploadedFiles.length;
    $('.upload-filelist .title h5').text(`Files (${fileCount})`);
    $('.btn-file span').text(`(${fileCount})`);
}

// 파일 개수 업데이트 함수
function updateFileCount2() {
    const fileCount = uploadedFiles2.length;
    $('.upload-filelist2 .title h5').text(`Files (${fileCount})`);
    $('.btn-file2 span').text(`(${fileCount})`);
}

// 파일 인풋 초기화
function resetFileInput($input) {
    $input.val('');
}

// 자동완성
let debounceTimeout;
let selectedSuggestionIndex = -1;
let suggestionsVisible = false;

function fetchSuggestions(inputId, apiUrl, suggestionId) {
    clearTimeout(debounceTimeout);
    debounceTimeout = setTimeout(() => {
        const inputField = document.getElementById(inputId);
        let suggestionsList = document.getElementById(suggestionId);

        if (!inputField || !suggestionsList) return;

        const inputClearDiv = inputField.parentElement;
        inputClearDiv.appendChild(suggestionsList);

        const query = inputField.value;
        if (query.length > 0) {
            fetch(`${apiUrl}?query=${encodeURIComponent(query)}&field=${encodeURIComponent(inputId)}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    const previousSelectedText = (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < data.length) ? data[selectedSuggestionIndex] : null;
                    suggestionsList.innerHTML = '';
                    if (data.length > 0) {
                        suggestionsVisible = true;
                        suggestionsList.style.display = 'block'; // 결과가 있을 때만 표시
                        data.forEach((item, index) => {
                            let li = document.createElement('li');
                            li.textContent = item;
                            li.setAttribute('data-index', index);
                            li.onclick = function () {
                                document.getElementById(inputId).value = this.textContent;
                                suggestionsList.innerHTML = '';
                                suggestionsList.style.display = 'none';
                                suggestionsVisible = false;
                            };
                            suggestionsList.appendChild(li);
                            // 이전에 선택한 항목을 다시 선택 상태로 설정
                            if (item === previousSelectedText) {
                                selectedSuggestionIndex = index;
                                li.classList.add('selected');
                            }
                        });
                    } else {
                        suggestionsList.style.display = 'none'; // 결과가 없으면 숨김
                        suggestionsVisible = false;
                        selectedSuggestionIndex = -1;
                    }
                })
                .catch(error => {
                    console.error('Fetch error:', error);
                    suggestionsList.style.display = 'none'; // 오류 시 숨김
                    suggestionsVisible = false;
                    selectedSuggestionIndex = -1;
                });
        } else {
            suggestionsList.innerHTML = '';
            suggestionsList.style.display = 'none';
            suggestionsVisible = false;
            selectedSuggestionIndex = -1;
        }
    }, 300);  // 300ms의 디바운스 타임 적용
}

// 자동완성 기능
function initializeAutoComplete(inputId, apiUrl, suggestionId) {
    const inputField = document.getElementById(inputId);

    let preventFetchOnEnter = false;  // Enter 키로 선택했을 때 검색을 방지하기 위한 플래그

    inputField.addEventListener('keyup', (event) => {
        if (event.key !== 'Enter' && event.key !== 'ArrowDown' && event.key !== 'ArrowUp' && !preventFetchOnEnter) {
            fetchSuggestions(inputId, apiUrl, suggestionId);
        }
        // Enter 키가 눌렸을 경우에는 검색을 방지
        preventFetchOnEnter = false;  // 플래그 초기화
    });

    inputField.addEventListener('keydown', function (event) {
        const suggestionsList = document.getElementById(suggestionId);
        const items = suggestionsList.getElementsByTagName('li');

        if (suggestionsVisible) {
            if (event.key === 'ArrowDown') {
                // 아래 화살표 키를 누르면
                event.preventDefault(); // 기본 동작 방지
                selectedSuggestionIndex = (selectedSuggestionIndex + 1) % items.length;
                updateSuggestionSelection(items);
            } else if (event.key === 'ArrowUp') {
                // 위 화살표 키를 누르면
                event.preventDefault(); // 기본 동작 방지
                selectedSuggestionIndex = (selectedSuggestionIndex - 1 + items.length) % items.length;
                updateSuggestionSelection(items);
            } else if (event.key === 'Enter') {
                // Enter 키를 누르면
                if (selectedSuggestionIndex >= 0) {
                    event.preventDefault(); // 기본 동작 방지
                    items[selectedSuggestionIndex].click();
                    preventFetchOnEnter = true;  // 엔터로 선택한 후 검색 방지
                    suggestionsVisible = false; // Enter 키를 누르면 선택을 확정하고 목록을 숨김
                    // inputField.blur(); // 입력 필드 포커스 해제
                }
            } else if (event.key === 'Escape') {
                // Escape 키를 누르면
                suggestionsList.style.display = 'none';
                suggestionsVisible = false;
            }
        }
    });

    // 포커스 해제 시 자동 완성 목록 숨기기
    inputField.addEventListener('blur', function () {
        setTimeout(function () {
            const suggestionsList = document.getElementById(suggestionId);
            suggestionsList.style.display = 'none';
            suggestionsVisible = false;
        }, 300); // 300ms 지연 시간
    });

    // 마우스 클릭으로 선택 시에도 검색 방지
    document.getElementById(suggestionId).addEventListener('click', function (event) {
        if (event.target.tagName === 'LI') {
            preventFetchOnEnter = true;
        }
    });
}

function updateSuggestionSelection(items) {
    // 모든 항목의 선택 상태 초기화
    for (let i = 0; i < items.length; i++) {
        items[i].classList.remove('selected');
    }

    // 현재 선택된 항목에 선택 상태 추가
    if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < items.length) {
        items[selectedSuggestionIndex].classList.add('selected');
        items[selectedSuggestionIndex].scrollIntoView({block: 'nearest'});
    }
}

// 공통코드 리스트 가져오기
function fetchCommonCodes(parentId, selectElementId) {
    fetch(`/api/common/find_parent_id?id=${parentId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok ' + response.statusText);
            }
            return response.json();
        })
        .then(data => {
            const selectElement = document.getElementById(selectElementId);

            // "전체" 옵션이 있는지 확인
            const allOption = selectElement.querySelector('option[value=""]:not([disabled]):not([hidden])');

            // "선택" 옵션이 있는지 확인
            const defaultOption = selectElement.querySelector('option[value=""][disabled][hidden]');

            // 기존 옵션 제거 (특정 옵션 제외)
            selectElement.innerHTML = ''; // 모든 옵션 제거

            // "전체" 옵션이 있으면 다시 추가 (선택 가능하게 설정)
            if (allOption) {
                allOption.selected = true; // 전체를 기본 선택으로 유지
                selectElement.appendChild(allOption);
            }

            // "선택" 옵션이 있으면 다시 추가 (선택 불가능하게 설정)
            if (defaultOption) {
                selectElement.appendChild(defaultOption);
            }

            data.forEach(item => {
                const option = document.createElement('option');
                option.value = item.id;
                option.textContent = item.Value; // item.value를 데이터베이스의 "Value" 필드로 변경
                selectElement.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Error fetching options:', error);
        });



}

// 로딩바를 보여주는 함수
function showLoader() {
    document.getElementById('loader2').style.display = 'block';
}

// 로딩바를 숨기는 함수
function hideLoader() {
    document.getElementById('loader2').style.display = 'none';
}

// 파일업로드 변수설정
let uploadedFiles2 = [];
// 삭제할 파일 목록
let deletedFiles2 = [];

$(document).ready(function () {
    function initializeUploadComponent2(component) {

        const $fileInput = $(component).find('.fileInput2');
        const $fileList = $(component).find('.filelist2');
        const $fileCountTitle = $(component).find('.upload-filelist2 .title h5');

        $fileInput.on('change', function (event) {
            const files = event.target.files;
            handleFileSelect2(files);
        });

        $(component).find('.upload-filebox2').on('dragover', function (event) {
            event.preventDefault();
            event.stopPropagation();
            $(this).addClass('dragging');
        });

        $(component).find('.upload-filebox2').on('dragleave', function (event) {
            event.preventDefault();
            event.stopPropagation();
            $(this).removeClass('dragging');
        });

        $(component).find('.upload-filebox2').on('drop', function (event) {
            event.preventDefault();
            event.stopPropagation();
            $(this).removeClass('dragging');
            // handleFileSelect(event.originalEvent.dataTransfer.files);

            const files = event.originalEvent.dataTransfer.files;

            handleFileSelect2(files);
            // 드래그 앤 드롭 후 파일 입력 요소 초기화
            resetFileInput($fileInput);
        });

        function handleFileSelect2(files) {
            $.each(files, function (index, file) {
                if (file.size > MAX_FILE_SIZE) {
                    Alert.alert('', '파일 크기는 1GB를 초과할 수 없습니다.');
                    resetFileInput($fileInput);
                } else if(!uploadedFiles2.some(f => f.name === file.name && f.size === file.size)) {
                    uploadedFiles2.push(file);
                    console.log('uploadedFiles2',uploadedFiles2);
                    const fileSize = (file.size / 1024).toFixed(2) + ' KiB';
                    const li = $('<li>').html(`
                    <p>${file.name} <span>(${fileSize})</span></p>
                    <a href="#" title="삭제" class="btn-file-delete2">
                        <img src="/images/icon/ico-filedelete.svg" alt="삭제아이콘">
                    </a>
                `);
                    $fileList.append(li);
                }
            });
            updateFileCount2();
        }

        $(component).on('click', '.btn-file-delete2', function (event) {
            event.preventDefault();
            const li = $(this).closest('li');
            const fileName = li.find('p').text().split(' (')[0];

            // 파일 이름과 일치하지 않는 파일들로 필터링하여 uploadedFiles 업데이트
            const removedFile = uploadedFiles2.find(file => file.name === fileName);
            uploadedFiles2 = uploadedFiles2.filter(file => file.name !== fileName);
            console.log('uploadedFiles2',uploadedFiles2);
            // 삭제된 파일을 deletedFiles에 추가
            if (removedFile) {
                deletedFiles2.push(removedFile);
                console.log("deletedFiles2", deletedFiles2);
            }
            li.remove();
            updateFileCount2();

            // 파일 선택 후 파일 입력 요소 초기화
            resetFileInput2($fileInput);
        });

        $(component).find('.btn-file-deleteall2').on('click', function (event) {
            event.preventDefault();
            // 모든 업로드된 파일을 삭제된 파일 리스트에 추가
            deletedFiles2 = deletedFiles2.concat(uploadedFiles2);

            $fileList.empty();
            uploadedFiles2 = [];
            updateFileCount2();
            resetFileInput2($fileInput);
        });

        function updateFileCount2() {
            const fileCount = uploadedFiles2.length;
            $fileCountTitle.text(`Files (${fileCount})`);
            $('.btn-file2 span').text(`(${fileCount})`);
        }

        function resetFileInput2($input) {
            $input.val('');
        }
    }

    $('.upload-component2').each(function () {
        initializeUploadComponent2(this);
    });
});

function closePopup(popupId) {
    // 해당 ID의 팝업 요소를 찾음
    const popup = document.getElementById(popupId);

    if (popup) {
        // 팝업을 포함한 모달 숨기기
        const modal = popup.closest('.modal');
        if (modal) {
            modal.style.display = 'none';
            modal.classList.remove('show');
        }

        // 팝업 숨기기
        popup.style.display = 'none';
        popup.classList.remove('show');
    }
}

function showPopup(element) {
    // 클릭된 요소의 data-popup 속성 값 가져오기
    const popupId = element.getAttribute('data-popup');

    // 해당 ID의 팝업 요소를 찾음
    const popup = document.getElementById(popupId);

    if (popup) {
        // 팝업을 포함한 모달을 표시
        const modal = popup.closest('.modal');
        if (modal) {
            modal.style.display = 'block';
        }

        // 팝업을 표시
        popup.style.display = 'block';

        // 애니메이션을 위한 클래스 추가
        requestAnimationFrame(() => {
            popup.classList.add('show');
            if (modal) {
                modal.classList.add('show');
            }
        });
    }
}

/*// 모달로 그래프 확장
$(document).ready(function() {

    // 모든 chart-wrap 요소에 투명한 오버레이를 자동으로 추가
    $('.chart-wrap').each(function() {
        // expand-icon 아이콘 생성
        let expandIcon = $('<span class="material-symbols-outlined">\n' +
            'zoom_out_map\n' +
            '</span>');

        // expand-icon 스타일 적용
        expandIcon.css({
            'position': 'absolute',
            'top': '17px', // 차트 오른쪽 끝 위에서 10px 아래
            'right': '3px', // 차트의 오른쪽 끝에 위치
            'cursor': 'pointer',
            'z-index': '10',
            'color': '##B3B3B3'
        });

        // chart-wrap에 expand-icon 추가
        $(this).css('position', 'relative').append(expandIcon);
    });


    // .chart-wrap에 클릭 이벤트 바인딩 (동적 요소에 대해서도 대응)
    $(document).on('click', '.material-symbols-outlined', function() {
        // 클릭한 div의 id 가져오기
        let chartHolderId = $(this).prev('div[id^="chartHolder"]').attr('id');

        // 모달 열기
        let modal = document.getElementById("chartModal");
        let modalChartHolder = document.getElementById("modalChartHolder");
        modal.style.display = "block";

        // 기존 차트의 레이아웃과 데이터를 모달 차트로 설정
        rMateChartH5.create(chartHolderId + "_modal", "modalChartHolder", "rMateOnLoadCallFunction=chartReadyHandler", "100%", "100%");

        // 모달 닫기 버튼 클릭 시 모달 닫기
        let closeModal = document.getElementsByClassName("close")[0];
        closeModal.onclick = function() {
            modal.style.display = "none";
            modalChartHolder.innerHTML = ""; // 모달 차트 초기화
        }

        // 모달 외부를 클릭하면 모달 닫기
        window.onclick = function(event) {
            if (event.target === modal) {
                modal.style.display = "none";
                modalChartHolder.innerHTML = ""; // 모달 차트 초기화
            }
        }
    });

    // 공통 모달 HTML을 body 태그에 추가
    const modalHTML = `
        <div id="chartModal" class="modal" style="opacity:100">
            <div class="modal-content">
                <span class="close">&times;</span>
                <div id="modalChartHolder" style="width: 100%; height: 500px;"></div>
            </div>
        </div>
    `;

    // body 태그의 끝에 모달을 추가
    $('body').append(modalHTML);

    // 모달 닫기 이벤트
    $(document).on('click', '.close', function () {
        $('#chartModal').hide();
        $('#modalChartHolder').html('');  // 모달 내용 초기화
    });

    $('div[id^="chartHolder"]').on('dblclick', function(event) {
        clickfunc(event);
    });

});*/

// .chart-wrap에 클릭 이벤트 바인딩 (동적 요소에 대해서도 대응)
// function clickfunc(event){
//     console.log(event);
//     // 클릭한 div의 id 가져오기
//     let chartHolderId = $(event.target).closest('div[id^="chartHolder"]').attr('id');
//
//     // ID가 제대로 추출되었는지 확인
//     console.log("Clicked chartHolder ID: " + chartHolderId);
//
//     // 모달 열기
//     let modal = document.getElementById("chartModal");
//     let modalChartHolder = document.getElementById("modalChartHolder");
//     modal.style.display = "block";
//
//     // 기존 차트의 레이아웃과 데이터를 모달 차트로 설정
//     rMateChartH5.create(chartHolderId + "_modal", "modalChartHolder", "rMateOnLoadCallFunction=chartReadyHandler", "100%", "100%");
//
//     // 모달 닫기 버튼 클릭 시 모달 닫기
//     let closeModal = document.getElementsByClassName("close")[0];
//     closeModal.onclick = function() {
//         modal.style.display = "none";
//         modalChartHolder.innerHTML = ""; // 모달 차트 초기화
//     }
//
//     // 모달 외부를 클릭하면 모달 닫기
//     window.onclick = function(event) {
//         if (event.target === modal) {
//             modal.style.display = "none";
//             modalChartHolder.innerHTML = ""; // 모달 차트 초기화
//         }
//     }
// }

// 모달로 그래프 확장
$(document).ready(function () {
    // 공통 모달 HTML 추가
    if (!$('#chartModal').length) {
        const modalHTML = `
            <div id="chartModal" class="modal" style="display: none; opacity: 1;">
                <div class="modal-content">
                    <span class="close">&times;</span>
                    <canvas id="modalChartHolder" style="width: 1230px; height: 490px; display: block; box-sizing: border-box;" width="1230" height="490"></canvas>
                </div>
            </div>
        `;
        $('body').append(modalHTML);
    }

    // 모든 chart-wrap 요소에 투명한 오버레이 및 클릭 이벤트 추가
    $('.chart-wrap').each(function () {
        let expandIcon = $('<span class="material-symbols-outlined">\n' +
            'zoom_out_map\n' +
            '</span>');

        expandIcon.css({
            'position': 'absolute',
            'top': '17px',
            'right': '3px',
            'cursor': 'pointer',
            'z-index': '1000',
            'color': '#B3B3B3'
        });

        $(this).css('position', 'relative').append(expandIcon);

        // 클릭 이벤트
        expandIcon.on('click', function () {
            let chartCanvas = $(this).siblings('canvas'); // 현재 chart-wrap의 canvas
            let chartInstance = chartCanvas.data('chart');

            if (!chartInstance) {
                console.error("차트를 찾을 수 없습니다. 차트를 먼저 생성해 주세요.");
                return;
            }

            // 모달 열기
            let modal = $('#chartModal');
            modal.css('display', 'flex'); // 모달 중앙 정렬을 위해 flex 사용

            // 모달의 2D 컨텍스트 가져오기
            let ctx = $('#modalChartHolder')[0].getContext('2d');

            // 기존 모달 차트 제거
            if (window.modalChartInstance) {
                window.modalChartInstance.destroy();
            }

            // 모달에 클릭된 차트의 데이터를 사용하여 새 차트 생성
            window.modalChartInstance = new Chart(ctx, {
                type: chartInstance.config.type,
                data: chartInstance.config.data,
                options: chartInstance.config.options
            });
        });
    });

    // 모달 닫기 이벤트
    $(document).on('click', '.close', function () {
        $('#chartModal').css('display', 'none');

        // 모달 차트 삭제
        if (window.modalChartInstance) {
            window.modalChartInstance.destroy();
            window.modalChartInstance = null;
        }
    });

    // 모달 외부 클릭 시 닫기
    $(window).on('click', function (event) {
        if ($(event.target).is('#chartModal')) {
            $('#chartModal').css('display', 'none');

            if (window.modalChartInstance) {
                window.modalChartInstance.destroy();
                window.modalChartInstance = null;
            }
        }
    });
});
