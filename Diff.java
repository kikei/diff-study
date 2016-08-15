public class Diff {
    public class Edit {
        public static final boolean INSERT = true;
        public static final boolean DELETE = false;

        public Edit prev;
        public int index1;
        public int index2;
        public boolean insert;

        public Edit() {
            this(false, -1, -1, null);
        }

        public Edit(boolean ins, int idx1, int idx2, Edit p) {
            insert = ins;
            index1 = idx1;
            index2 = idx2;
            prev = p;
        }

        public void reverse() {
            insert = !insert;
            int t = index1;
            index1 = index2;
            index2 = t;
            if (prev != null) prev.reverse();
        }

    } // class Edit

    final String[] mArr1;
    final String[] mArr2;
    final boolean mReversed;

    int mDistance;
    Edit[] mRoute;

    public Levenshtein(String[] arr1, String[] arr2) {
        if (arr1.length > arr2.length) {
            mArr1 = arr2;
            mArr2 = arr1;
            mReversed = true;
        } else {
            mArr1 = arr1;
            mArr2 = arr2;
            mReversed = false;
        }

        mDistance = -1;
        mRoute = null;
    }

    public int getDistance() {
        if (mDistance == -1) calcDistance();
        return mDistance;
    }

    /*
     * 編集手続きを実行する順序に並べ替えて返す。
     * 実行順序は以下の通り:
     * 1. 削除をindexが大きい順に実行
     *    (同名のIDの削除と挿入がある場合、先に挿入をすると同じIDが2つになり区別できなくなる)
     * 2. 挿入をindexが大きい順に実行
     * 削除・挿入ともに、indexが大きい順に実行することで、より小さいindexの手続きの影響を
     * 考慮する必要がなくなる。
     */
    public Edit[] getEditList() {
        if (mDistance == -1) calcDistance();

        // 連結リストになっているEditをArrayに変換する
        Edit[] edits = getRowEditList();

        // 並べ替え
        edits = sortEditList(edits);

        // 追加差分の先頭indexを調べる
        int p;
        for (p = 0; p < mDistance; p++)
            if (edits[p].insert) break;

        // 追加差分のindexから削除分だけ減らす
        for (int i = 0; i < p; i++) {
            int index = edits[i].index1;
            for (int j = p; j < mDistance; j++)
                if (index < edits[j].index1) edits[j].index1 -= 1;
        }

        return edits;
    }

    public Edit[] sortEditList(Edit[] edits) {
            
        Comparator<Edit> editComparator = new Comparator<Edit>() {
                final int M = Math.max(mArr1.length, mArr2.length) + 1;
                public int compare(Edit e1, Edit e2) {
                    return
                        ((e2.insert ? 0 : M) + e2.index1) -
                        ((e1.insert ? 0 : M) + e1.index1);

                }
            };
        Arrays.sort(edits, editComparator);
            
        return edits;
    }

    public Edit[] getRowEditList() {
        if (mDistance == -1) calcDistance();

        Edit[] edits = new Edit[mDistance];
        if (mArr2.length + 1 < mRoute.length) {
            Edit e = mRoute[mArr2.length + 1];
            for (int i = 0; i < edits.length; i++, e = e.prev) {
                edits[i] = new Edit(e.insert, e.index1, e.index2, null);
            }
        }

        return edits;
    }

    public String[] apply(String[] arr) {
        Edit[] edits = getEditList();

        String[] arr1, arr2;
        if (mReversed) {
            arr1 = mArr2;
            arr2 = mArr1;
        } else {
            arr1 = mArr1;
            arr2 = mArr2;
        }

        LinkedList<String> list = new LinkedList<String>(Arrays.asList(arr));

        for (int i = 0; i < edits.length; i++) {
            Edit e = edits[i];
            if (e.insert) {
                if (e.index1 < list.size()) {
                    list.add(e.index1, arr2[e.index2]);
                } else {
                    list.addLast(arr2[e.index2]);
                }				
            } else {
                list.remove(arr1[e.index1]);
            }
        }
        return list.toArray(new String[0]);
    }
        
    private int calcDistance() {

        if (mArr1.length == 0 && mArr2.length == 0) {
            mRoute = new Edit[1];
            mDistance = 0;
            return mDistance;
        }

        String[] arr1 = mArr1;
        String[] arr2 = mArr2;

        int offset = arr1.length + 1;
        int delta = arr2.length - arr1.length;
        int p, k, y;

        int[] fp = new int[(arr1.length + 1) * (arr2.length + 1) * 2];
        Edit[] rt = new Edit[fp.length];

        for (int i = 0; i < fp.length; i++) fp[i] = -1;

        for (p = 0; fp[delta+offset] != arr2.length; p++) {
            for (k = -p; k < delta; k++) {
                if (fp[k-1+offset] + 1 > fp[k+1+offset]) {
                    y = fp[k-1+offset] + 1;
                    rt[k+offset] = new Edit(Edit.INSERT, y-k, y-1, rt[k-1+offset]);
                } else {
                    y = fp[k+1+offset];
                    rt[k+offset] = new Edit(Edit.DELETE, y-k-1, y, rt[k+1+offset]);
                }
                fp[k + offset] = snake(k, y, arr1, arr2);
            }

            for (k = delta + p; k > delta; k--) {
                if (fp[k-1+offset] + 1 > fp[k+1+offset]) {
                    y = fp[k-1+offset] + 1;
                    rt[k+offset] = new Edit(Edit.INSERT, y-k, y-1, rt[k-1+offset]);
                } else {
                    y = fp[k+1+offset];
                    rt[k+offset] = new Edit(Edit.DELETE, y-k-1, y, rt[k+1+offset]);
                }
                fp[k + offset] = snake(k, y, arr1, arr2);
            }

            if (fp[delta-1+offset] + 1 > fp[delta+1+offset]) {
                y = fp[delta-1+offset] + 1;
                rt[delta+offset] = new Edit(Edit.INSERT, y-k, y-1, rt[delta-1+offset]);
            } else { 
                y = fp[delta+1+offset];
                rt[delta+offset] =
                    new Edit(Edit.DELETE, y-k-1, y, rt[delta+1+offset]);
            }
            fp[delta + offset] = snake(delta, y, arr1, arr2);
        }

        mRoute = rt;

        Edit e = mRoute[delta+offset];
        if (mReversed) e.reverse();

        mDistance = delta + (p - 1) * 2;
        return mDistance;
    }

    private int snake(int k, int y, String[] arr1, String[] arr2) {
        int x = y - k;
        while (x < arr1.length && y < arr2.length && arr1[x].equals(arr2[y])) {
            x++;
            y++;
        }
        return y;
    }
}
