/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.core.controllers.tracking.events.media;

import android.support.annotation.NonNull;
import com.waz.zclient.core.controllers.tracking.attributes.Attribute;
import com.waz.zclient.core.controllers.tracking.attributes.CompletedMediaType;
import com.waz.zclient.core.controllers.tracking.events.Event;

public class CompletedMediaActionEvent extends Event {


    public CompletedMediaActionEvent(CompletedMediaType type, String conversationType, boolean withBot, boolean isEphemeral, String ephemeralExpiration) {
        attributes.put(Attribute.ACTION, type.nameString);
        attributes.put(Attribute.CONVERSATION_TYPE, conversationType);
        attributes.put(Attribute.WITH_OTTO, String.valueOf(withBot));
        attributes.put(Attribute.IS_EPHEMERAL, String.valueOf(isEphemeral));
        attributes.put(Attribute.EPHEMERAL_EXPIRATION, ephemeralExpiration);
    }

    @NonNull
    @Override
    public String getName() {
        return "media.completed_media_action";
    }
}
