let GridUtil = {
    changeOrder: function (val, grid) {
        if (!grid || !grid.selectedItems || grid.selectedItems.length === 0) {
            Alert.alert('', '순서를 변경할 Row 를 선택하세요.');
            return;
        }

        let collectionView = grid.collectionView;
        if (!collectionView || !collectionView.items) {
            console.error("CollectionView가 정의되지 않았습니다.");
            return;
        }

        if (val === "U") {
            let items = grid.selectedItems;
            items.forEach((item) => {
                let self_index = collectionView.items.indexOf(item);
                let upper_index = self_index - 1;
                if (upper_index < 0) {
                    Alert.alert('', "첫번째 Row가 선택되었습니다.");
                    return;
                }

                // ✅ 배열에서 항목 위치 변경
                [collectionView.items[self_index], collectionView.items[upper_index]] =
                    [collectionView.items[upper_index], collectionView.items[self_index]];

                collectionView.refresh();
                grid.select(new wijmo.grid.CellRange(upper_index, 0)); // ✅ 수정
            });
        } else {
            let items = grid.selectedItems;
            items.reverse(); // 아래로 내릴 때는 역순으로 루프를 돌려야 한다.
            items.forEach((item) => {
                let self_index = collectionView.items.indexOf(item);
                let under_index = self_index + 1;
                if (under_index >= collectionView.items.length) {
                    Alert.alert('', "마지막 Row가 선택되었습니다.");
                    return;
                }

                // ✅ 배열에서 항목 위치 변경
                [collectionView.items[self_index], collectionView.items[under_index]] =
                    [collectionView.items[under_index], collectionView.items[self_index]];

                collectionView.refresh();
                grid.select(new wijmo.grid.CellRange(under_index, 0)); // ✅ 수정
            });
        }
    },

    adjustHeight: function (grid, rows_len) {
        if (!grid || !grid.hostElement) {
            console.error("Grid가 정의되지 않았습니다.");
            return;
        }
        let rowHeight = grid.rows.defaultSize;
        let headerHeight = grid.columnHeaders.rows.defaultSize;
        let height = headerHeight + (rowHeight * (rows_len + 2));
        if (height < 150) height = 150;
        grid.hostElement.style.height = height + 'px';
    }
};
