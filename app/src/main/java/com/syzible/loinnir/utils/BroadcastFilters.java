package com.syzible.loinnir.utils;

/**
 * Created by ed on 10/07/2017.
 */

public enum BroadcastFilters {
    finish_main_activity {
        @Override
        public String toString() {
            return "com.syzible.loinnir.finish_main_activity";
        }
    },

    new_locality_info_update {
        @Override
        public String toString() {
            return "com.syzible.loinnir.new_locality_update";
        }
    },

    new_partner_message {
        @Override
        public String toString() {
            return "com.syzible.loinnir.new_partner_message";
        }
    }
}
