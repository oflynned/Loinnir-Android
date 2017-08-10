package com.syzible.loinnir.network;

/**
 * Created by ed on 16/12/2016
 */

abstract class PostRequest<T> extends Request<T> {
    PostRequest(NetworkCallback<T> networkCallback, T payload, String url, boolean isExternal) {
        super(networkCallback, payload, url, "POST", isExternal);
    }
}