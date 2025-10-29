package com.example.onlyfanshop.model.response;



import com.example.onlyfanshop.model.UserDTO;

import java.util.List;

public class UserPageResponse {
    private List<UserDTO> content;
    private int totalPages;
    private int totalElements;
    private int size;
    private int number;

    public List<UserDTO> getContent() {
        return content;
    }

    public void setContent(List<UserDTO> content) {
        this.content = content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}

