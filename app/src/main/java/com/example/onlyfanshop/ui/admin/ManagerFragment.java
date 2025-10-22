package com.example.onlyfanshop.ui.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.ui.product.ProductManagementActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ManagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ManagerFragment extends Fragment {
    private LinearLayout btnUserManagement, btnProductManagement;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public ManagerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ManagerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ManagerFragment newInstance(String param1, String param2) {
        ManagerFragment fragment = new ManagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager, container, false);

        // LẤY USERNAME TỪ SHARED PREFERENCES
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", null);

        // TÌM VÀ SET USERNAME CHO tvUserName
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        if (username != null) {
            tvUserName.setText(username);
        } else {
            tvUserName.setText("Name");
        }

        // Các nút quản lý như cũ
        btnUserManagement = view.findViewById(R.id.btnUserManagement);
        btnProductManagement = view.findViewById(R.id.btnProductManagement);

        btnProductManagement.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProductManagementActivity.class);
            startActivity(intent);
        });

        return view;
    }
}